"""Test only: execute production Lua using fakeredis + Lupa; no cloud credentials."""
import json
import sys
import fakeredis

store = fakeredis.FakeRedis(decode_responses=True)
for line in sys.stdin:
    try:
        args = json.loads(line)
        result = store.execute_command(*args)
        # redis-py maps RESP +OK from SET to True; the production REST adapter returns "OK".
        if args[0].upper() == "SET" and result is True:
            result = "OK"
        print(json.dumps({"result": result}), flush=True)
    except Exception:
        print(json.dumps({"error": "fixture_command_failed"}), flush=True)
