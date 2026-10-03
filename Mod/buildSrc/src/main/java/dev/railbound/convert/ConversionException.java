package dev.railbound.convert;

/** A model that breaks the trainset model rules; the message names every problem found. */
public class ConversionException extends Exception {
    public ConversionException(String message) {
        super(message);
    }
}
