"""Test only: execute production Lua using fakeredis + Lupa; no cloud credentials."""
import json
import sys
import fakeredis

store = fakeredis.FakeRedis(decode_responses=True)
for line in sys.stdin:
    try:
        result = store.execute_command(*json.loads(line))
        print(json.dumps({"result": result}), flush=True)
    except Exception:
        print(json.dumps({"error": "fixture_command_failed"}), flush=True)
