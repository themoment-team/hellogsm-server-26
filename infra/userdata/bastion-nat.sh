#!/bin/bash
set -euxo pipefail

# 1. IP forwarding 영구 활성화
cat <<'EOF' > /etc/sysctl.d/99-nat.conf
net.ipv4.ip_forward = 1
EOF
sysctl -p /etc/sysctl.d/99-nat.conf

# 2. iptables 설치 (Amazon Linux 2023 기본 미설치)
dnf install -y iptables iptables-services

# 3. NAT(MASQUERADE) 규칙 설정 - VPC 내부(172.16.0.0/24)에서 나가는 트래픽을 외부로 마스커레이딩
PRIMARY_IF=$(ip route show default | awk '{print $5}' | head -n1)
iptables -t nat -A POSTROUTING -o "$PRIMARY_IF" -s 172.16.0.0/24 -j MASQUERADE
iptables -A FORWARD -i "$PRIMARY_IF" -o "$PRIMARY_IF" -m state --state RELATED,ESTABLISHED -j ACCEPT

# 4. 재부팅 시에도 유지되도록 규칙 저장
mkdir -p /etc/sysconfig
iptables-save > /etc/sysconfig/iptables
systemctl enable iptables
systemctl start iptables
