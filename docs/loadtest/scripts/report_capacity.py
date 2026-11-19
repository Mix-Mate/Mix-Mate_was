#!/usr/bin/env python3
"""용량 테스트 결과(k6 JSON, CPU csv, Hikari JMX csv)를 마크다운 표로 만든다.
usage: report_capacity.py <results-dir>

CPU 는 코어 1개 대비 %를 2초 간격으로 쟀다(cpusampler.py). 표에는 '코어 환산 (전체 대비 %)'로 적는다."""
import csv, json, os, sys

R = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.environ.get('LT_WORK', '/tmp/mixmate-loadtest'), 'results')
ORDER = ['smoke', 'baseline', 'load', 'spike', 'stress', 'breakpoint']
LOAD = {
    'smoke': '2 TPS, 15초',
    'baseline': '10 TPS, 1분',
    'load': '1분 동안 10→100 TPS, 이후 100 TPS로 2분 유지',
    'spike': '10초 동안 10→150 TPS, 이후 150 TPS로 1분 유지',
    'stress': '50→400 TPS까지 30초 단위로 50씩 증가',
    'breakpoint': '500→3000 TPS까지 30초 단위로 500씩 증가',
}


def ms(v):
    return '-' if v is None else (f'{v / 1000:.2f}s' if v >= 1000 else f'{v:.1f}ms')


def cpu_max(path):
    mx = {'app': 0.0, 'mysql': 0.0, 'k6': 0.0}
    if os.path.exists(path):
        for row in csv.reader(open(path)):
            if len(row) == 4:
                for k, v in zip(('app', 'mysql', 'k6'), row[1:]):
                    try:
                        mx[k] = max(mx[k], float(v))
                    except ValueError:
                        pass
    return mx


def hikari_max(path):
    act = wait = 0
    if os.path.exists(path):
        for r in csv.DictReader(open(path)):
            act, wait = max(act, int(r['active'])), max(wait, int(r['waiting']))
    return act, wait


rows = []
for sc in ORDER:
    jf = os.path.join(R, f'capacity-{sc}.json')
    if not os.path.exists(jf):
        continue
    d = json.load(open(jf))['metrics']
    dur, fail = d['http_req_duration'], d['http_req_failed']
    ncpu = int(open(os.path.join(R, f'capacity-{sc}.ncpu')).read().strip() or 1)
    cpu = cpu_max(os.path.join(R, f'capacity-{sc}.cpu.csv'))
    act, wait = hikari_max(os.path.join(R, f'capacity-{sc}.hikari.csv'))

    def fmt(v):
        return f'{v / 100:.1f}코어 ({v / ncpu:.0f}%)'

    rows.append((sc, d['http_reqs']['count'], dur['p(95)'], dur['p(99)'], dur['max'], fail['value'] * 100,
                 d.get('dropped_iterations', {}).get('count', 0), fmt(cpu['app']), fmt(cpu['mysql']), fmt(cpu['k6']), act, wait))

print('| 시나리오 | 설정 부하 | 총 요청 | k6 p95 | k6 p99 | 최대 | 오류율 | 미전송(dropped) | 앱 CPU 최대 | MySQL CPU 최대 | k6 CPU 최대 | Hikari active 최대 / waiting 최대 (풀 20) |')
print('| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |')
for sc, reqs, p95, p99, mx, err, dropped, ca, cm, ck, act, wait in rows:
    print(f'| {sc} | {LOAD[sc]} | {reqs:.0f} | {ms(p95)} | {ms(p99)} | {ms(mx)} | {err:.2f}% | {dropped:.0f} | {ca} | {cm} | {ck} | {act} / {wait} |')
