#!/usr/bin/env python3
"""OSIV 실험 시계열(ts-*.k6.csv, ts-*.hikari.csv)로 비교 그래프를 만든다.
usage: charts.py <results-dir> <out-dir>

그래프(위→아래): HikariCP 풀(사용 중 / 대기) · 초당 성공 응답 수 · API 응답 시간(로그 축)
왼쪽 = OSIV on, 오른쪽 = OSIV off. 같은 구독자 수·같은 풀 크기에서 OSIV만 다르다.

시간축 기준: k6 워밍업 시나리오의 첫 완료 시각을 0초로 삼는다(워밍업은 SSE 연결 전이라 항상 즉시 성공).
k6 csv 의 timestamp 는 요청 '완료' 시각이라, 타임아웃(10초)으로 끝난 요청은 시작보다 10초 늦게 찍힌다."""
import csv, os, sys
from collections import defaultdict

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np
from matplotlib import font_manager

R = sys.argv[1]
OUT = sys.argv[2]
os.makedirs(OUT, exist_ok=True)

for name in ('Apple SD Gothic Neo', 'AppleGothic', 'NanumGothic', 'Malgun Gothic'):
    if name in {f.name for f in font_manager.fontManager.ttflist}:
        plt.rcParams['font.family'] = name
        break
plt.rcParams['axes.unicode_minus'] = False

XMAX = 55


def load_k6(path):
    """(scenario, 완료 시각(초), 지연 ms, status) 목록."""
    rows = []
    with open(path, newline='') as f:
        for r in csv.DictReader(f):
            if r['metric_name'] == 'http_req_duration' and r.get('scenario'):
                rows.append((r['scenario'], float(r['timestamp']), float(r['metric_value']), r.get('status', '')))
    return rows


def load_hikari(path):
    with open(path, newline='') as f:
        return [(int(r['epoch_ms']) / 1000, int(r['active']), int(r['waiting'])) for r in csv.DictReader(f)]


def draw_pair(left, right, title, fname, pool, holders):
    fig, axes = plt.subplots(3, 2, figsize=(13, 9.5), sharex='col')
    cases = [(left, 'OSIV 켬  (open-in-view=true)', '#c0392b'), (right, 'OSIV 끔  (open-in-view=false)', '#1e8449')]
    wait_max = 210

    for col, (case, label, color) in enumerate(cases):
        k6 = load_k6(os.path.join(R, f'ts-{case}.k6.csv'))
        hk = load_hikari(os.path.join(R, f'ts-{case}.hikari.csv'))
        t0 = min(r[1] for r in k6 if r[0] == 'warmup')
        api = [r for r in k6 if r[0] == 'api']

        # 1) HikariCP: 사용 중(왼쪽 축) / 커넥션 대기 스레드(오른쪽 축)
        ax = axes[0][col]
        ts = [h[0] - t0 for h in hk]
        act = [h[1] for h in hk]
        ax.fill_between(ts, act, color=color, alpha=0.3)
        ax.plot(ts, act, color=color, lw=2, label='사용 중 연결(active)')
        ax.axhline(pool, color='gray', lw=1, ls=':')
        ax.set_ylim(0, pool + 2)
        ax.set_ylabel(f'사용 중 연결 수 (풀 크기 {pool})')
        ax.set_title(label, color=color, fontsize=12, fontweight='bold', pad=26)
        ax2 = ax.twinx()
        ax2.plot(ts, [h[2] for h in hk], color='black', lw=1.5, ls='--', label='커넥션 대기 스레드(waiting)')
        ax2.set_ylim(0, wait_max)
        ax2.set_ylabel('대기 스레드 수')
        h1, l1 = ax.get_legend_handles_labels()
        h2, l2 = ax2.get_legend_handles_labels()
        ax.legend(h1 + h2, l1 + l2, loc='center right', fontsize=8)

        # 2) 초당 성공 응답 (완료 기준)
        ax = axes[1][col]
        succ = defaultdict(int)
        for _, ts_, ms, st in api:
            if st == '200':
                succ[int(ts_ - t0)] += 1
        xs = list(range(0, XMAX))
        ys = [succ.get(x, 0) for x in xs]
        ax.bar(xs, ys, color=color, alpha=0.85, width=0.9)
        ax.set_ylabel('성공 응답 수 (건/초)')
        ax.set_ylim(0, 30)
        total_ok = sum(ys)
        ax.text(0.98, 0.9, f'성공 {total_ok}건 / 전송 {len(api)}건', transform=ax.transAxes, ha='right', va='top',
                fontsize=10, fontweight='bold', color=color)

        # 3) 응답 시간 (요청별 점 + 초당 p95)
        ax = axes[2][col]
        ax.scatter([r[1] - t0 for r in api], [max(r[2], 0.5) for r in api], s=6, color=color, alpha=0.35)
        per_sec = defaultdict(list)
        for _, ts_, ms, _ in api:
            per_sec[int(ts_ - t0)].append(ms)
        px = sorted(per_sec)
        ax.plot(px, [np.percentile(per_sec[x], 95) for x in px], color='black', lw=1.4, label='초당 p95')
        ax.set_yscale('log')
        ax.set_ylim(1, 30000)
        ax.axhline(10000, color='gray', lw=1, ls=':')
        ax.text(0.5, 12000, 'k6 요청 타임아웃 10초', color='gray', fontsize=8)
        ax.set_ylabel('API 응답 시간 (ms, 로그 축)')
        ax.set_xlabel('테스트 시작 후 경과 시간 (초) — 점은 요청 완료 시각')
        ax.legend(loc='center left', fontsize=8)

        for row in range(3):
            axes[row][col].axvline(6, color='orange', lw=1, ls='--')
            axes[row][col].axvline(12, color='steelblue', lw=1, ls='--')
            axes[row][col].set_xlim(0, XMAX)
        axes[0][col].annotate(f'6초: SSE 구독자 {holders}명 연결', xy=(6, 1.0), xycoords=('data', 'axes fraction'),
                              xytext=(0, 4), textcoords='offset points', color='darkorange', fontsize=8, ha='left')
        axes[0][col].annotate('12초: API 20 req/s 시작', xy=(12, 1.0), xycoords=('data', 'axes fraction'),
                              xytext=(4, 15), textcoords='offset points', color='steelblue', fontsize=8, ha='left')

    fig.suptitle(title, fontsize=14, fontweight='bold')
    fig.tight_layout(rect=[0, 0, 1, 0.965])
    fig.savefig(os.path.join(OUT, fname), dpi=140)
    plt.close(fig)
    print('saved', os.path.join(OUT, fname))


pairs = [
    ('A-h15', 'D-h15', 'SSE 구독자 15명 · 커넥션 풀 10 — OSIV 켬(왼쪽) vs 끔(오른쪽)', 'osiv-pool10-holders15.png', 10, 15),
    ('B-h30', 'C-h30', 'SSE 구독자 30명 · 커넥션 풀 20 — OSIV 켬(왼쪽) vs 끔(오른쪽)', 'osiv-pool20-holders30.png', 20, 30),
]
for left, right, title, fname, pool, holders in pairs:
    if all(os.path.exists(os.path.join(R, f'ts-{c}.{k}.csv')) for c in (left, right) for k in ('k6', 'hikari')):
        draw_pair(left, right, title, fname, pool, holders)
    else:
        print('skip (시계열 파일 없음):', left, right)
