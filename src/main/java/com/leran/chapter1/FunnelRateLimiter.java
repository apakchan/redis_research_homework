package com.leran.chapter1;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FunnelRateLimiter {
    private static class Funnel {
        // 漏斗容量
        private int capacity;
        // 漏嘴流水速率
        private float leakingRate;
        // 漏斗剩余空间
        private int leftQuota;
        // 上一次漏水时间
        private long leakingTs;

        public Funnel(int capacity, float leakingRate) {
            this.capacity = capacity;
            this.leakingRate = leakingRate;
            this.leftQuota = capacity;
            this.leakingTs = System.currentTimeMillis();
        }

        public void makeSpace() {
            long nowTs = System.currentTimeMillis();
            int leaking = (int) ((nowTs - this.leakingTs) * leakingRate);
            if (leaking < 0) {
                // 上一次 makeSpace 距今太久, 溢出
                this.leftQuota = capacity;
                this.leakingTs = nowTs;
            }
            if (leaking < 1) {
                // 上一次 makeSpace 间隔太短, leaking 最小为 1
                return;
            }
            this.leftQuota += leaking;
            this.leakingTs = nowTs;
            if (this.leftQuota > this.capacity) {
                this.leftQuota = this.capacity;
            }
        }

        // 灌水, 有流量进来了
        public boolean watering(int quota) {
            // 每次灌水需要 makeSpace 一下
            makeSpace();
            if (this.leftQuota >= quota) {
                this.leftQuota -= quota;
                return true;
            }
            return false;
        }
    }

    private final Map<String, Funnel> funnels = new HashMap<>();

    public boolean isActionAllowed(String userId, String actionKey, int capacity, float leakingRate) {
        String key = String.format("hist:%s:%s", userId, actionKey);
        if (funnels.get(key) == null) {
            funnels.put(key, new Funnel(capacity, leakingRate));
        }
        return funnels.get(key).watering(1);
    }
}
