package hortonworks.hdp.refapp.trucking.storm.bolt.alert;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.windowing.TupleWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TumblingWindowInfractionCountBolt extends BaseWindowedBolt {


	private static final long serialVersionUID = 8257758116690028179L;
	private static final Logger LOG = LoggerFactory.getLogger(TumblingWindowInfractionCountBolt.class);
	
    private OutputCollector collector;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }	
	
	@Override
	public void execute(TupleWindow inputWindow) {
		
		
		Map<TruckDriver, TruckDriverInfractionCount> truckDriverInfractions = new HashMap<TruckDriver, TruckDriverInfractionCount>();
			
		List<Tuple> tuplesInWindow = inputWindow.get();
		LOG.debug("Events in Current Window: " + tuplesInWindow.size());
		
		for(Tuple tuple: tuplesInWindow) {
			
			int driverId = tuple.getIntegerByField("driverId");
			String driverName = tuple.getStringByField("driverName");
			int routeId = tuple.getIntegerByField("routeId");
			String routeName = tuple.getStringByField("routeName");
			int truckId = tuple.getIntegerByField("truckId");
			Timestamp eventTime = (Timestamp) tuple.getValueByField("eventTime");
			String eventType = tuple.getStringByField("eventType");
			double longitude = tuple.getDoubleByField("longitude");
			double latitude = tuple.getDoubleByField("latitude");
			long correlationId = tuple.getLongByField("correlationId");		
			
			
			if(isInfractionEvent(eventType)) {
				LOG.debug("Infraction Event["+ eventType + "] for driver["+driverId+"], truck["+truckId +"]");
				
				TruckDriver driver = new TruckDriver(driverId, truckId);
				TruckDriverInfractionCount count = truckDriverInfractions.get(driver);
				if(count == null) {
					count = new TruckDriverInfractionCount(driver);
					truckDriverInfractions.put(driver, count);
				}
				count.addInfraction(eventType);
			}
		}
		
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("About to output result from Infraction Count Tumbling Window");
			LOG.debug("Total Number of Driver Infractions from Output Window is" + truckDriverInfractions.size());
			for(TruckDriver truckDriver: truckDriverInfractions.keySet()) {
				LOG.debug(truckDriverInfractions.get(truckDriver).toString());
			}
		}
		
		
		collector.emit(new Values(truckDriverInfractions));

	}

	
	private boolean isInfractionEvent(String eventType) {
		return !"Normal".equals(eventType);
	}
	
	 @Override
	    
	 public void declareOutputFields(OutputFieldsDeclarer declarer) {
	        
		 declarer.declare(new Fields("driversInfractionCounts"));
	    
	 }	

	


}