package com.addthis.hydra.task.map;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.Bundles;
import com.addthis.bundle.core.list.ListBundle;
import com.addthis.bundle.util.AutoField;
import com.addthis.bundle.value.ValueObject;
import com.addthis.bundle.value.ValueTranslationException;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RepeatBuilder extracts a numeric field from a bundle, then emits a copy of the bundle the specified number of times.
 */
public class RepeatBuilder extends StreamBuilder {

    private static final Logger log = LoggerFactory.getLogger(RepeatBuilder.class);

    /** The field describing how many times to emit each bundle */
    @JsonProperty(required = true) private AutoField repeatField;

    /** A default number of times to emit if the specified field is missing or can't be parsed */
    @JsonProperty private long defaultRepeats = 0;

    /** If true, escalate parse failures rather than using the default repeat number */
    @JsonProperty private boolean failOnParseException = false;

    /** If true, perform a deep-copy on each input bundle prior to sending it downstream */
    @JsonProperty private boolean copyBundle = false;

    @Override
    public void init() {}

    @Override
    public void process(Bundle row, StreamEmitter emitter) {
        long emitTimes = defaultRepeats;
        ValueObject repeatCount = repeatField.getValue(row);
        if (repeatCount != null) {
            try {
                emitTimes = (int) repeatCount.asLong().getLong();
            } catch (ValueTranslationException vte) {
                if (failOnParseException) {
                    throw vte;
                } else {
                    log.warn("Failed to parse repeats field value={}", repeatCount.asString().asNative());
                }
            }
        }
        for (int i=0; i<emitTimes; i++) {
            Bundle toEmit = copyBundle ? Bundles.deepCopyBundle(row) : row;
            // Copy the bundle each time to ensure sanity downstream
            emitter.emit(toEmit);
        }
    }
}
