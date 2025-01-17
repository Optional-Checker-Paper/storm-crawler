/**
 * Licensed to DigitalPebble Ltd under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.digitalpebble.stormcrawler.solr.bolt;

import com.digitalpebble.stormcrawler.solr.SolrConnection;
import java.io.IOException;
import java.util.Map;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * Deletion bolt for Apache SOLR.
 * Must be connected to the 'deletion' stream of the status updater bolt.
 * <pre>
 * 		builder.setBolt("deleter", new DeletionBolt()).localOrShuffleGrouping("status", Constants.DELETION_STREAM_NAME);
 * </pre>
 **/
@SuppressWarnings("serial")
public class DeletionBolt extends BaseRichBolt {

    private static final Logger LOG = LoggerFactory.getLogger(DeletionBolt.class);

    private static final String BOLT_TYPE = "indexer";

    private OutputCollector _collector;

    private SolrConnection connection;

    @Override
    public void prepare(
            Map<String, Object> conf, TopologyContext context, OutputCollector collector) {
        _collector = collector;
        try {
            connection = SolrConnection.getConnection(conf, BOLT_TYPE);
        } catch (Exception e) {
            LOG.error("Can't connect to Solr", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cleanup() {
        if (connection != null)
            try {
                connection.close();
            } catch (SolrServerException | IOException e) {
                LOG.warn("Faled to cloase connection", e);
            }
    }

    @Override
    public void execute(Tuple tuple) {
        String url = tuple.getStringByField("url");
        try {
            connection.getClient().deleteById(url);
        } catch (SolrServerException | IOException e) {
            _collector.fail(tuple);
            LOG.error("Exception caught while deleting", e);
            return;
        }
        _collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer arg0) {
        // none
    }
}
