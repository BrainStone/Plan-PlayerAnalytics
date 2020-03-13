/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.gathering.timed;

import com.djrapitops.plan.gathering.ServerSensor;
import com.djrapitops.plan.gathering.SystemUsage;
import com.djrapitops.plan.gathering.domain.builders.TPSBuilder;
import com.djrapitops.plan.identification.ServerInfo;
import com.djrapitops.plan.settings.config.PlanConfig;
import com.djrapitops.plan.settings.config.paths.DataGatheringSettings;
import com.djrapitops.plan.storage.database.DBSystem;
import com.djrapitops.plan.storage.database.transactions.events.TPSStoreTransaction;
import com.djrapitops.plan.utilities.analysis.Average;
import com.djrapitops.plan.utilities.analysis.Maximum;
import com.djrapitops.plan.utilities.analysis.TimerAverage;
import com.djrapitops.plugin.logging.console.PluginLogger;
import com.djrapitops.plugin.logging.error.ErrorHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * TPSCounter extension for game server platforms.
 *
 * @author Rsl1122
 */
@Singleton
public class ServerTPSCounter<W> extends TPSCounter {

    private final boolean noDirectTPS;
    private final ServerSensor<W> serverSensor;
    private final DBSystem dbSystem;
    private final ServerInfo serverInfo;
    private TPSCalculator indirectTPS;
    private TimerAverage directTPS;
    private Maximum.ForInteger playersOnline;
    private Average cpu;
    private Average ram;

    @Inject
    public ServerTPSCounter(
            ServerSensor<W> serverSensor,
            PlanConfig config,
            DBSystem dbSystem,
            ServerInfo serverInfo,
            PluginLogger logger,
            ErrorHandler errorHandler
    ) {
        super(config.get(DataGatheringSettings.DISK_SPACE), logger, errorHandler);

        noDirectTPS = !serverSensor.supportsDirectTPS();
        this.serverSensor = serverSensor;
        this.dbSystem = dbSystem;
        this.serverInfo = serverInfo;
        if (noDirectTPS) {
            indirectTPS = new TPSCalculator();
        } else {
            directTPS = new TimerAverage();
        }
        playersOnline = new Maximum.ForInteger(0);
        cpu = new Average();
        ram = new Average();
    }

    @Override
    public void pulse() {
        long time = System.currentTimeMillis();
        Optional<Double> result = pulseTPS(time);
        playersOnline.add(serverSensor.getOnlinePlayerCount());
        cpu.add(SystemUsage.getAverageSystemLoad());
        ram.add(SystemUsage.getUsedMemory());
        result.ifPresent(tps -> save(tps, time));
    }

    private void save(double averageTPS, long time) {
        long timeLastMinute = time - TimeUnit.MINUTES.toMillis(1L);
        int maxPlayers = playersOnline.getMaxAndReset();
        double averageCPU = cpu.getAverageAndReset();
        long averageRAM = (long) ram.getAverageAndReset();
        int entityCount = 0;
        int chunkCount = 0;
        for (W world : serverSensor.getWorlds()) {
            entityCount += serverSensor.getEntityCount(world);
            chunkCount += serverSensor.getChunkCount(world);
        }
        long freeDiskSpace = getFreeDiskSpace();

        dbSystem.getDatabase().executeTransaction(new TPSStoreTransaction(
                serverInfo.getServerUUID(),
                TPSBuilder.get()
                        .date(timeLastMinute)
                        .tps(averageTPS)
                        .playersOnline(maxPlayers)
                        .usedCPU(averageCPU)
                        .usedMemory(averageRAM)
                        .entities(entityCount)
                        .chunksLoaded(chunkCount)
                        .freeDiskSpace(freeDiskSpace)
                        .toTPS()
        ));
    }

    public Optional<Double> pulseTPS(long time) {
        if (noDirectTPS) {
            return indirectTPS.pulse(time);
        } else {
            if (directTPS.add(time, serverSensor.getTPS())) {
                return Optional.of(directTPS.getAverageAndReset(time));
            } else {
                return Optional.empty();
            }
        }
    }
}
