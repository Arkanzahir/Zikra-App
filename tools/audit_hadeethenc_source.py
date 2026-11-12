"""Read-only structural audit of a privately downloaded official HadeethEnc snapshot.

No network calls, data import or religious correctness certification. Reports
contain counts, IDs and hashes rather than the downloaded text corpus.
"""
import argparse
from collections import Counter
import csv
import hashlib
import json
from pathlib import Path
import re
import sqlite3
import time


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def read_json(path):
    if path.stat().st_size > 1_048_576:
        raise ValueError('JSON snapshot exceeds audit budget')
    return json.loads(path.read_text(encoding='utf-8'))


def audit(snapshot, samples, csv_export):
    if not 0 < snapshot.stat().st_size <= 30_000_000:
        raise ValueError('SQLite snapshot exceeds audit budget')
    connection = sqlite3.connect(snapshot.resolve().as_uri() + '?mode=ro&immutable=1', uri=True)
    connection.row_factory = sqlite3.Row
    deadline = time.monotonic() + 15
    connection.set_progress_handler(lambda: int(time.monotonic() > deadline), 1000)
    connection.execute('PRAGMA trusted_schema=OFF')
    connection.execute('PRAGMA query_only=ON')
    try:
        integrity = connection.execute('PRAGMA integrity_check').fetchone()[0]
        count = connection.execute('SELECT count(*) FROM hadeeths').fetchone()[0]
        if count > 20_000:
            raise ValueError('Row count exceeds audit budget')
        rows = [dict(row) for row in connection.execute('SELECT * FROM hadeeths ORDER BY id')]
        required = ['title', 'hadeeth', 'hadeeth_ar', 'grade', 'attribution']
        optional = ['explanation', 'hints']
        empty = {field: sum(not str(row[field] or '').strip() for row in rows)
                 for field in required + optional}
        columns = [row['name'] for row in connection.execute('PRAGMA table_info(hadeeths)')]
        coverage = []
        for root in connection.execute('SELECT id FROM categories WHERE parent_id=0 ORDER BY id'):
            total = connection.execute('''WITH RECURSIVE tree(id) AS (
                SELECT id FROM categories WHERE id=?
                UNION SELECT c.id FROM categories c JOIN tree t ON c.parent_id=t.id)
                SELECT count(DISTINCT ch.hadeeth_id) FROM tree
                JOIN category_hadeeth ch ON ch.category_id=tree.id''', (root['id'],)).fetchone()[0]
            coverage.append({'categoryId': root['id'], 'uniqueHadithsIncludingDescendants': total})
        root_api = read_json(samples / 'categories-id.json')
        consistent_categories = all(next(int(item['hadeeths_count']) for item in root_api
                                        if int(item['id']) == root['categoryId']) ==
                                    root['uniqueHadithsIncludingDescendants'] for root in coverage)
        selection = read_json(samples / 'selection.json')
        if (not isinstance(selection.get('ids'), list) or not 1 <= len(selection['ids']) <= 20
                or any(type(identifier) is not int or not 0 < identifier <= 1_000_000
                       for identifier in selection['ids'])):
            raise ValueError('Invalid sample ID selection')
        if not 0 < csv_export.stat().st_size <= 20_000_000:
            raise ValueError('CSV exceeds audit budget')
        with csv_export.open(encoding='utf-8-sig', newline='') as stream:
            reader = csv.reader(stream)
            metadata = next(reader)[0]
            csv_columns = next(reader)
            csv_rows = []
            for row in reader:
                if not row:
                    continue
                if len(row) != len(csv_columns) or len(csv_rows) >= 20_000:
                    raise ValueError('Invalid CSV shape or row budget')
                csv_rows.append(dict(zip(csv_columns, row)))
        release = re.search(r'# Last update: ([^\r\n]+) \(v([0-9.]+)\)', metadata)
        update_link = re.search(r'# Check for updates: (https://hadeethenc\.com/[^\r\n]+)', metadata)
        if not release or not update_link:
            raise ValueError('Missing source release metadata')
        comparisons = []
        by_id = {row['id']: row for row in rows}
        mapping = {'title': 'title', 'hadith_text': 'hadeeth', 'hadith_text_ar': 'hadeeth_ar',
                   'explanation': 'explanation', 'grade': 'grade', 'takhrij': 'attribution'}
        csv_ids = [int(row['id']) for row in csv_rows]
        if set(csv_ids) != set(by_id):
            raise ValueError('CSV and SQLite ID sets differ')
        mismatches = {key: sum(row[key] != str(by_id[int(row['id'])][field] or '')
                              for row in csv_rows) for key, field in mapping.items()}
        bracket_matches = {key: sum(row[key].strip('[]') ==
                                   str(by_id[int(row['id'])][field] or '') for row in csv_rows)
                           for key, field in [('grade', 'grade'), ('takhrij', 'attribution')]}
        for identifier in selection['ids']:
            translated = read_json(samples / f'sample-{identifier}-id.json')
            arabic = read_json(samples / f'sample-{identifier}-ar.json')
            row = by_id[identifier]
            hints = json.loads(row['hints']) if row['hints'] else []
            comparisons.append({
                'id': identifier,
                'textFieldsMatchOffline': all(translated.get(field) == row[field]
                    for field in required + ['explanation']),
                'hintsMatchAfterJsonDecoding': hints == translated.get('hints'),
                'embeddedArabicMatchesArabicApi': translated.get('hadeeth_ar') == arabic.get('hadeeth'),
                'arabicReferenceNonempty': bool(arabic.get('reference')),
                'indonesianReferencePresent': bool(translated.get('reference')),
                'apiTranslationVersionFields': [key for key in translated if re.search('version', key, re.I)],
                'offlineArabicVersion': row['ar_version'],
            })
        first = read_json(samples / 'list-id-page1.json')
        second = read_json(samples / 'list-id-page2.json')
        first_ids = [item['id'] for item in first['data']]
        second_ids = [item['id'] for item in second['data']]
        dangling = connection.execute('''SELECT count(*) FROM category_hadeeth ch
            LEFT JOIN categories c ON c.id=ch.category_id
            LEFT JOIN hadeeths h ON h.id=ch.hadeeth_id WHERE c.id IS NULL OR h.id IS NULL''').fetchone()[0]
        duplicate_links = connection.execute('''SELECT count(*) FROM (
            SELECT category_id,hadeeth_id FROM category_hadeeth
            GROUP BY category_id,hadeeth_id HAVING count(*)>1)''').fetchone()[0]
        return {
            'schemaVersion': 1,
            'scope': 'Structural inspection of one official Indonesian export and ten bilingual API samples; not independent tashih, complete-book proof or integration approval.',
            'snapshot': {'bytes': snapshot.stat().st_size, 'sha256': digest(snapshot),
                         'integrity': integrity, 'hadithCount': len(rows),
                         'uniqueIds': len(by_id), 'minimumId': min(by_id), 'maximumId': max(by_id),
                         'columns': columns, 'emptyFields': empty,
                         'arabicTextWithoutArabicScript': sum(not re.search(r'[\u0600-\u06ff]', row['hadeeth_ar']) for row in rows),
                         'arabicVersions': dict(Counter(row['ar_version'] for row in rows)),
                         'distinctSourceGradeLabels': len({row['grade'] for row in rows}),
                         'labelsContainingHasan': sum('hasan' in row['grade'].lower() for row in rows),
                         'sourceCommentCannotFindNamedScholarGradeIds': [row['id'] for row in rows
                             if 'tidak menemukan' in row['grade'].lower()],
                         'danglingCategoryLinks': dangling, 'duplicateCategoryPairs': duplicate_links,
                         'detailedReferenceColumnPresent': 'reference' in columns,
                         'indonesianTranslationVersionColumnPresent': any(key in columns for key in ('version', 'id_version', 'translation_version'))},
            'rootCategories': coverage, 'rootCategoryCountsMatchLiveApi': consistent_categories,
            'samples': comparisons, 'sampleSelection': selection['rule'],
            'officialCsvRelease': {'bytes': csv_export.stat().st_size, 'sha256': digest(csv_export),
                                   'sourceVersion': release.group(2), 'declaredLastUpdate': release.group(1),
                                   'updateCheckUrl': update_link.group(1),
                                   'metadataPreservationNoticePresent': "DON'T REMOVE" in metadata,
                                   'dataRows': len(csv_rows), 'uniqueIds': len(set(csv_ids)),
                                   'allIndonesian': all(row['lang'] == 'id' for row in csv_rows),
                                   'idSetMatchesSqlite': True, 'columns': csv_columns,
                                   'rawFieldMismatchCountsAgainstSqlite': mismatches,
                                   'matchingGradeAndAttributionAfterBracketComparisonOnly': bracket_matches,
                                   'comparisonDoesNotModifyOriginalContent': True},
            'pagination': {'firstPageCount': len(first_ids), 'secondPageCount': len(second_ids),
                           'duplicateIdsWithinPages': len(first_ids + second_ids) - len(set(first_ids + second_ids)),
                           'categoryTotal': first['meta']['total_items'],
                           'lastPage': first['meta']['last_page'], 'onlyTwoPagesFetched': True},
            'missingExplanationIds': [row['id'] for row in rows if not str(row['explanation'] or '').strip()],
            'distributionApproved': False, 'b01CompleteBooksSatisfied': False,
        }
    finally:
        connection.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--snapshot', type=Path, required=True)
    parser.add_argument('--samples', type=Path, required=True)
    parser.add_argument('--csv', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    report = audit(args.snapshot, args.samples, args.csv)
    with args.report.open('x', encoding='utf-8') as stream:
        json.dump(report, stream, indent=2)
        stream.write('\n')
    print(json.dumps({'entries': report['snapshot']['hadithCount'], 'samples': len(report['samples']),
                      'integrity': report['snapshot']['integrity'], 'distributionApproved': False}))


if __name__ == '__main__':
    main()
