#!/usr/bin/env python3
"""OSIV 실험 결과(k6 summary-export JSON)에서 type:api 서브메트릭만 뽑아 마크다운 표로 만든다.
usage: report.py <results-dir> [케이스이름 ...]   (이름을 주면 그 케이스만, 예: A-h15 D-h15)"""
import glob, json, os, re, sys

R = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.environ.get('LT_WORK', '/tmp/mixmate-loadtest'), 'results')
ONLY = set(sys.argv[2:])
CFG = {'A': ('on', 10), 'B': ('on', 20), 'C': ('off', 20), 'D': ('off', 10)}


def fmt_ms(v):
    if v is None:
        return '-'
    return f'{v / 1000:.1f}s' if v >= 1000 else f'{v:.0f}ms'


rows = []
for f in glob.glob(os.path.join(R, '*.json')):
    name = os.path.basename(f)[:-5]
    m = re.match(r'([A-D])-h(\d+)$', name)
    if not m or (ONLY and name not in ONLY):
        continue
    cfg, holders = m.group(1), int(m.group(2))
    d = json.load(open(f))['metrics']
    dur = d.get('http_req_duration{type:api}', {})
    fail = d.get('http_req_failed{type:api}', {})
    sent = d.get('http_reqs{type:api}', {}).get('count', 0)
    dropped = d.get('dropped_iterations', {}).get('count', 0)
    tf = os.path.join(R, name + '.threads')
    thr = open(tf).read().strip().split('=')[1] if os.path.exists(tf) else '-'
    ok = (1 - fail['value']) * 100 if fail else None
    rows.append((cfg, holders, sent, ok, dur.get('avg'), dur.get('p(95)'), dur.get('p(99)'), dropped, thr))

print('| 설정 | OSIV | 풀 | SSE 구독자 | 전송한 API 요청 | 성공률 | 평균 | p95 | p99 | 미전송(dropped) | 커넥션 대기 스레드 |')
print('| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |')
for cfg, h, sent, ok, avg, p95, p99, dropped, thr in sorted(rows, key=lambda r: (r[0], r[1])):
    osiv, pool = CFG[cfg]
    ok_s = f'{ok:.1f}%' if ok is not None else '-'
    print(f'| {cfg} | {osiv} | {pool} | {h} | {sent:.0f} | {ok_s} | {fmt_ms(avg)} | {fmt_ms(p95)} | {fmt_ms(p99)} | {dropped:.0f} | {thr} |')
