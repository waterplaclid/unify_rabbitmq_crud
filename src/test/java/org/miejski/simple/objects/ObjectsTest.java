package org.miejski.simple.objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miejski.simple.objects.events.ObjectCreation;
import org.miejski.simple.objects.events.ObjectUpdate;
import org.miejski.simple.objects.serdes.GenericField;
import org.miejski.simple.objects.serdes.GenericSerde;

import java.util.Properties;

public class ObjectsTest {

    private final String numberKey = "1";
    private static TopologyTestDriver testDriver;
    private static KeyValueStore<String, ObjectState> store;

    private ConsumerRecordFactory<String, ObjectCreation> createRecordFactory = new ConsumerRecordFactory<>(new StringSerializer(), ObjectsCreationSerde.serde().serializer());
    private ConsumerRecordFactory<String, ObjectUpdate> updateRecordFactory = new ConsumerRecordFactory<>(new StringSerializer(), ObjectsUpdateSerde.serde().serializer());
    private ConsumerRecordFactory<String, GenericField> genericObjectFactory = new ConsumerRecordFactory<>(new StringSerializer(), GenericSerde.serde().serializer());

    @BeforeEach
    void setUp() {
        Topology topology = new ObjectsTopology().buildTopology();

        Properties props = new Properties();
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, this.getClass().getCanonicalName());
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName());

        testDriver = new TopologyTestDriver(topology, props);
        store = testDriver.getKeyValueStore(ObjectsTopology.OBJECTS_STORE_NAME);
    }

    @Test
    void shouldProperlyUseGenericObjectsInOneTopic() throws JsonProcessingException {
        ConsumerRecord<byte[], byte[]> genericCreate = genericObjectFactory.create(ObjectsTopology.FINAL_TOPIC, numberKey, GenericSerde.toGenericField(new ObjectCreation(20)));

        testDriver.pipeInput(genericCreate);

        ObjectState state = store.get(numberKey);
        Assertions.assertEquals(20, state.value);


        ConsumerRecord<byte[], byte[]> genericUpdate = genericObjectFactory.create(ObjectsTopology.FINAL_TOPIC, numberKey, GenericSerde.toGenericField(new ObjectUpdate(7)));
        testDriver.pipeInput(genericUpdate);

        state = store.get(numberKey);
        Assertions.assertEquals(7, state.value);

    }
}