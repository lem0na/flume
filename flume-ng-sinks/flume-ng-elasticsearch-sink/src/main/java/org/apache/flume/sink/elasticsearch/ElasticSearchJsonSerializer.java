/**
 * Created by lem0na on 23/09/2014.
 */

package org.apache.flume.sink.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.conf.ComponentConfiguration;
import org.elasticsearch.common.xcontent.XContentBuilder;

import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;

public class ElasticSearchJsonSerializer implements
        ElasticSearchEventSerializer {
    @Override
    public void configure(Context context) {
        // NO-OP...
    }

    @Override
    public void configure(ComponentConfiguration conf) {
        // NO-OP...
    }

    @Override
    public XContentBuilder getContentBuilder(Event event) throws IOException {
        XContentBuilder builder = jsonBuilder().startObject();
        appendBody(builder, event);
        //appendHeaders(builder, event);
        return builder;
    }


    private void appendBody(final XContentBuilder builder, final Event event)
            throws IOException, UnsupportedEncodingException {
        addComplexField(builder, "body", XContentType.JSON, event.getBody());
    }

    private void appendHeaders(final XContentBuilder builder, final Event event)
            throws IOException {
        final Map<String, String> headers = event.getHeaders();
        for (final String key : headers.keySet()) {
            ContentBuilderUtil.appendField(builder, key, headers.get(key).getBytes(charset));
        }
    }

    public void addComplexField(final XContentBuilder builder,
                                final String fieldName, final XContentType contentType,
                                final byte[] data) throws IOException {
        builder.field(fieldName, JsonXContent.jsonXContent.createParser(data).mapAndClose());
    }
}
