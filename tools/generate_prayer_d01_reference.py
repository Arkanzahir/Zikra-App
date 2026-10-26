"""Generate comparison fixtures with Astral 3.2; never imports the app's solar equations."""
from datetime import date, datetime, timedelta, timezone
from importlib.metadata import version
from math import atan, ceil, degrees, radians, tan
from pathlib import Path
from zoneinfo import ZoneInfo
import argparse
import json
from astral import Observer, SunDirection
from astral.julian import julianday, julianday_to_juliancentury
from astral.sun import elevation, noon, sun_declination, time_at_elevation


def asr(observer, day, zone):
    first = noon(observer, day, zone)
    if elevation(observer, first, with_refraction=False) <= 0:
        return None

    def residual(instant):
        declination = sun_declination(julianday_to_juliancentury(julianday(instant.astimezone(timezone.utc))))
        target = degrees(atan(1 / (1 + tan(radians(abs(observer.latitude - declination))))))
        return elevation(observer, instant, with_refraction=False) - target

    previous = first
    for minutes in range(10, 721, 10):
        following = first + timedelta(minutes=minutes)
        if residual(previous) >= 0 and residual(following) <= 0:
            for _ in range(40):
                middle = previous + (following - previous) / 2
                if residual(middle) > 0:
                    previous = middle
                else:
                    following = middle
            return following
        previous = following
    return None


def times(latitude, longitude, zone_name, day):
    observer = Observer(latitude, longitude, 0)
    zone = ZoneInfo(zone_name)
    results = []
    for index, altitude in enumerate([-20, -0.833, None, None, -0.833, -18]):
        try:
            if index == 2:
                instant = noon(observer, day, zone)
            elif index == 3:
                instant = asr(observer, day, zone)
            else:
                instant = time_at_elevation(observer, altitude, day,
                    SunDirection.RISING if index < 2 else SunDirection.SETTING,
                    zone, with_refraction=False)
        except ValueError:
            instant = None
        if instant is None:
            results.append(None)
        else:
            midnight = datetime.combine(instant.date(), datetime.min.time(), zone)
            minute = ceil((instant - midnight).total_seconds() / 60) + (0 if index == 1 else 2)
            results.append(f'{(minute // 60) % 24:02d}:{minute % 60:02d}')
    return results


def main():
    if version('astral') != '3.2':
        raise SystemExit('Use astral==3.2 for reproducible references')
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    cities = [
        ('Jakarta', -6.1754, 106.8272, 'Asia/Jakarta'),
        ('Surabaya', -7.280, 112.790, 'Asia/Jakarta'),
        ('Makassar', -5.1477, 119.4327, 'Asia/Makassar'),
        ('Jayapura', -2.5337, 140.7181, 'Asia/Jayapura'),
        ('London', 51.5074, -0.1278, 'Europe/London'),
        ('New York', 40.7128, -74.0060, 'America/New_York'),
        ('Sydney', -33.8688, 151.2093, 'Australia/Sydney'),
        ('Apia', -13.8333, -171.75, 'Pacific/Apia'),
        ('Tromso', 69.6492, 18.9553, 'Europe/Oslo'),
    ]
    rows = []
    for label, latitude, longitude, zone in cities:
        for value in ['2026-01-15', '2026-04-15', '2026-07-15', '2026-10-26', '2028-02-29']:
            rows.append(dict(city=label, latitude=latitude, longitude=longitude, zone=zone,
                date=value, times=times(latitude, longitude, zone, date.fromisoformat(value))))
    report = dict(generator='Astral 3.2, independent implementation of related solar formulae',
        method='Sea level, geometric altitudes -20/-0.833/noon/Asr shadow1/-0.833/-18; ceil minute, +2 except sunrise',
        toleranceMinutes=2, officialReligiousTimetable=False, rows=rows)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(f'Generated {len(rows)} reference days / {len(rows) * 6} events')


if __name__ == '__main__':
    main()
