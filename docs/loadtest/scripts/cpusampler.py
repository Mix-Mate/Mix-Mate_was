#!/usr/bin/env python3
"""앱·MySQL·k6 프로세스의 CPU 사용량을 2초마다 CSV로 남긴다.

macOS 의 `ps %cpu` 는 프로세스 수명 평균이라 순간 부하를 못 잡는다(400 TPS 에서도 몇 % 로 나온다).
그래서 누적 CPU 시간(cputime)의 증가량 ÷ 경과 시간으로 계산한다. 값의 단위는 '코어 1개 대비 %'이다.
(예: 250 = 코어 2.5개 분량을 쓰는 중)

usage: cpusampler.py <out.csv> <app_pid> <mysql_pidfile> [interval_sec=2]
columns: epoch, app, mysql, k6"""
import subprocess, sys, time

out, app_pid, mysql_pidfile = sys.argv[1], sys.argv[2], sys.argv[3]
interval = float(sys.argv[4]) if len(sys.argv) > 4 else 2.0


def cputime(pid):
    """누적 CPU 시간(초). 프로세스가 없으면 None."""
    if not pid:
        return None
    r = subprocess.run(['ps', '-o', 'cputime=', '-p', str(pid)], capture_output=True, text=True)
    s = r.stdout.strip()
    if not s:
        return None
    total = 0.0
    for part in s.replace('-', ':').split(':'):   # [d-]hh:mm:ss.ss
        total = total * 60 + float(part)
    return total


def k6_pid():
    r = subprocess.run(['pgrep', '-x', 'k6'], capture_output=True, text=True)
    return r.stdout.split()[0] if r.stdout.split() else None


def mysql_pid():
    try:
        return open(mysql_pidfile).read().strip()
    except OSError:
        return None


prev = {}
prev_t = time.time()
with open(out, 'w') as f:
    while True:
        time.sleep(interval)
        now = time.time()
        dt = now - prev_t
        prev_t = now
        row = [int(now)]
        for key, pid in (('app', app_pid), ('mysql', mysql_pid()), ('k6', k6_pid())):
            cur = cputime(pid)
            pct = 0.0
            if cur is not None and key in prev and prev[key][0] == pid:
                pct = max(0.0, (cur - prev[key][1]) / dt * 100)
            if cur is not None:
                prev[key] = (pid, cur)
            row.append(f'{pct:.1f}')
        f.write(','.join(map(str, row)) + '\n')
        f.flush()
