package org.miejski.questions;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.miejski.TopologyBuilder;
import org.miejski.questions.events.QuestionCreated;
import org.miejski.questions.events.QuestionUpdated;
import org.miejski.simple.objects.serdes.GenericField;
import org.miejski.simple.objects.serdes.GenericFieldSerde;

import java.util.HashMap;

public class QuestionsTopology implements TopologyBuilder {

    public static final String CREATE_TOPIC = "question_create_topic";
    public static final String UPDATE_TOPIC = "question_update_topic";
    public static final String DELETE_TOPIC = "question_delete_topic";
    public static final String FINAL_TOPIC = "question_state_topic";
    public static final String QUESTIONS_STORE_NAME = "questionsStateStore";


    @Override
    public Topology buildTopology() {
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        HashMap<String, Class> serializers = new HashMap<>();
        serializers.put(QuestionCreated.class.getSimpleName(), QuestionCreated.class);
        serializers.put(QuestionUpdated.class.getSimpleName(), QuestionUpdated.class);

        KStream<String, GenericField> allGenerics = streamsBuilder.stream(FINAL_TOPIC, Consumed.with(Serdes.String(), GenericFieldSerde.serde()));

        QuestionsAggregator aggregator = new QuestionsAggregator(serializers);

        Materialized<String, QuestionState, KeyValueStore<Bytes, byte[]>> store = Materialized.<String, QuestionState, KeyValueStore<Bytes, byte[]>>as(QUESTIONS_STORE_NAME).withKeySerde(Serdes.String()).withValueSerde(QuestionStateSerde.questionStateSerde());

        allGenerics.groupByKey()
                .aggregate(QuestionState::new, aggregator, store);
        return streamsBuilder.build();
    }
}
