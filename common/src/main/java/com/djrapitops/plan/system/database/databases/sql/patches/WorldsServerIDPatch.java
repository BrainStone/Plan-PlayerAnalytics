package com.djrapitops.plan.system.database.databases.sql.patches;

import com.djrapitops.plan.system.database.databases.sql.SQLDB;
import com.djrapitops.plan.system.database.databases.sql.processing.ExecStatement;
import com.djrapitops.plan.system.database.databases.sql.processing.QueryAllStatement;
import com.djrapitops.plan.system.database.databases.sql.processing.QueryStatement;
import com.djrapitops.plan.system.database.databases.sql.tables.WorldTable;
import com.djrapitops.plan.system.database.databases.sql.tables.WorldTimesTable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WorldsServerIDPatch extends Patch {

    public WorldsServerIDPatch(SQLDB db) {
        super(db);
    }

    @Override
    public boolean hasBeenApplied() {
        String tableName = WorldTable.TABLE_NAME;
        String columnName = WorldTable.Col.SERVER_ID.get();
        return hasColumn(tableName, columnName)
                && allValuesHaveServerID(tableName, columnName);
    }

    private Boolean allValuesHaveServerID(String tableName, String columnName) {
        String sql = "SELECT COUNT(*) as c FROM " + tableName + " WHERE " + columnName + "=?";
        return query(new QueryStatement<Boolean>(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setInt(1, 0);
            }

            @Override
            public Boolean processResults(ResultSet set) throws SQLException {
                return set.next() && set.getInt("c") == 0;
            }
        });
    }

    @Override
    public void apply() {
        WorldTable worldTable = db.getWorldTable();

        List<UUID> serverUUIDs = db.getServerTable().getServerUUIDs();

        Map<UUID, Set<String>> worldsPerServer = new HashMap<>();
        for (UUID serverUUID : serverUUIDs) {
            worldsPerServer.put(serverUUID, worldTable.getWorldNamesOld(serverUUID));
        }

        for (Map.Entry<UUID, Set<String>> entry : worldsPerServer.entrySet()) {
            UUID serverUUID = entry.getKey();
            Set<String> worlds = entry.getValue();

            worldTable.saveWorlds(worlds, serverUUID);
        }

        updateWorldTimesTableWorldIDs();
        db.executeUnsafe("DELETE FROM " + WorldTable.TABLE_NAME + " WHERE " + WorldTable.Col.SERVER_ID + "=0");
    }

    private void updateWorldTimesTableWorldIDs() {
        List<WorldObj> worldObjects = getWorldObjects();
        Map<WorldObj, List<WorldObj>> oldToNewMap =
                worldObjects.stream()
                        .filter(worldObj -> worldObj.serverId == 0)
                        .collect(Collectors.toMap(
                                Function.identity(),
                                oldWorld -> worldObjects.stream()
                                        .filter(worldObj -> worldObj.serverId != 0)
                                        .filter(worldObj -> worldObj.equals(oldWorld))
                                        .collect(Collectors.toList()
                                        )));

        WorldTimesTable worldTimesTable = db.getWorldTimesTable();
        String sql = "UPDATE " + worldTimesTable + " SET " +
                WorldTimesTable.Col.WORLD_ID + "=?" +
                " WHERE " + WorldTimesTable.Col.WORLD_ID + "=?" +
                " AND " + WorldTimesTable.Col.SERVER_ID + "=?";
        db.executeBatch(new ExecStatement(sql) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                for (Map.Entry<WorldObj, List<WorldObj>> entry : oldToNewMap.entrySet()) {
                    WorldObj old = entry.getKey();
                    for (WorldObj newWorld : entry.getValue()) {
                        statement.setInt(1, newWorld.id);
                        statement.setInt(2, old.id);
                        statement.setInt(3, newWorld.serverId);
                        statement.addBatch();
                    }
                }
            }
        });
    }

    public List<WorldObj> getWorldObjects() {
        String sql = "SELECT * FROM " + WorldTable.TABLE_NAME;
        return query(new QueryAllStatement<List<WorldObj>>(sql, 100) {
            @Override
            public List<WorldObj> processResults(ResultSet set) throws SQLException {
                List<WorldObj> objects = new ArrayList<>();
                while (set.next()) {
                    int worldID = set.getInt(WorldTable.Col.ID.get());
                    int serverID = set.getInt(WorldTable.Col.SERVER_ID.get());
                    String worldName = set.getString(WorldTable.Col.NAME.get());
                    objects.add(new WorldObj(worldID, serverID, worldName));
                }
                return objects;
            }
        });
    }
}

class WorldObj {
    final int id;
    final int serverId;
    final String name;

    public WorldObj(int id, int serverId, String name) {
        this.id = id;
        this.serverId = serverId;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldObj worldObj = (WorldObj) o;
        return Objects.equals(name, worldObj.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", name='" + name + '\'' +
                '}';
    }
}
