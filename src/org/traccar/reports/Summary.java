package org.traccar.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

import org.traccar.Context;
import org.traccar.helper.DistanceCalculator;
import org.traccar.model.Position;
import org.traccar.reports.model.SummaryReport;
import org.traccar.web.CsvBuilder;
import org.traccar.web.JsonConverter;

public final class Summary {

    private Summary() {
    }

    private static SummaryReport calculateGeneralResult(long deviceId, Date from, Date to) throws SQLException {
        SummaryReport result = new SummaryReport();
        result.setDeviceId(deviceId);
        result.setDeviceName(Context.getDeviceManager().getDeviceById(deviceId).getName());
        Collection<Position> positions = Context.getDataManager().getPositions(deviceId, from, to);
        if (positions != null && !positions.isEmpty()) {
            Position previousPosition = null;
            double speedSum = 0;
            for (Position position : positions) {
                if (previousPosition != null) {
                    result.addDistance(DistanceCalculator.distance(previousPosition.getLatitude(),
                            previousPosition.getLongitude(), position.getLatitude(), position.getLongitude()));
                    if (position.getAttributes().get(Position.KEY_IGNITION) != null
                            && Boolean.parseBoolean(position.getAttributes().get(Position.KEY_IGNITION).toString())
                            && previousPosition.getAttributes().get(Position.KEY_IGNITION) != null
                            && Boolean.parseBoolean(previousPosition.getAttributes()
                                    .get(Position.KEY_IGNITION).toString())) {
                        result.addMotorHours(position.getFixTime().getTime()
                                - previousPosition.getFixTime().getTime());
                    }
                }
                previousPosition = position;
                speedSum += position.getSpeed();
                result.setMaxSpeed(position.getSpeed());
            }
            result.setAverageSpeed(speedSum / positions.size());
            result.setDistance(new BigDecimal(result.getDistance()).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }
        return result;
    }

    public static String getJson(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {
        JsonArrayBuilder json = Json.createArrayBuilder();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            json.add(JsonConverter.objectToJson(calculateGeneralResult(deviceId, from, to)));
        }
        return json.build().toString();
    }

    public static String getCsv(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {
        CsvBuilder csv = new CsvBuilder();
        csv.addHeaderLine(new SummaryReport());
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            csv.addLine(calculateGeneralResult(deviceId, from, to));
        }
        return csv.build();
    }
}
