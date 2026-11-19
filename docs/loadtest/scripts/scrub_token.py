#!/usr/bin/env python3
"""k6 --summary-export JSON 에서 setup() 반환값(setup_data: 로그인 토큰이 들어 있음)을 지운다.
결과 파일을 레포에 보관하기 전에 반드시 거친다.
usage: scrub_token.py <file.json> [...]"""
import json, sys

for path in sys.argv[1:]:
    with open(path, encoding='utf-8') as f:
        data = json.load(f)
    if data.pop('setup_data', None) is not None:
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write('\n')
        print('scrubbed', path)
