/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.jinbooks.util;

import java.time.Instant;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * Façade over Hutool {@link Snowflake} for distributed IDs.
 */
public class SnowFlakeId {

    private final Snowflake snowflake;

    private long datacenterId;
    private long machineId;
    private long sequence;
    private long lastStmp = -1L;
    private String dateTime;

    public SnowFlakeId(long datacenterId, long machineId) {
        this.datacenterId = datacenterId;
        this.machineId = machineId;
        this.snowflake = IdUtil.getSnowflake(machineId, datacenterId);
    }

    public SnowFlakeId(long datacenterId, long machineId, long sequence, long lastStmp) {
        this(datacenterId, machineId);
        this.sequence = sequence;
        this.lastStmp = lastStmp;
        if (lastStmp > 0) {
            dateTime = Instant.ofEpochMilli(lastStmp).toString();
        }
    }

    public synchronized long nextId() {
        long id = snowflake.nextId();
        lastStmp = snowflake.getGenerateDateTime(id);
        sequence = id & ~(-1L << 12L);
        return id;
    }

    public long currId() {
        return nextId();
    }

    public SnowFlakeId parse(long id) {
        long workerId = snowflake.getWorkerId(id);
        long dataCenterId = snowflake.getDataCenterId(id);
        long timeLong = snowflake.getGenerateDateTime(id);
        long sequenceInt = id & ~(-1L << 12L);
        return new SnowFlakeId(dataCenterId, workerId, sequenceInt, timeLong);
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(long datacenterId) {
        this.datacenterId = datacenterId;
    }

    public long getMachineId() {
        return machineId;
    }

    public void setMachineId(long machineId) {
        this.machineId = machineId;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public long getLastStmp() {
        return lastStmp;
    }

    public void setLastStmp(long lastStmp) {
        this.lastStmp = lastStmp;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
