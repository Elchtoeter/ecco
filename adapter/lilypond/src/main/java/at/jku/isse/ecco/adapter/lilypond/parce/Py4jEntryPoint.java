package at.jku.isse.ecco.adapter.lilypond.parce;

import at.jku.isse.ecco.adapter.lilypond.LilypondPlugin;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class Py4jEntryPoint {
    private static final Logger LOGGER = Logger.getLogger(LilypondPlugin.class.getName());

    private ConcurrentLinkedQueue<Py4jParseEvent> eventBuffer;
    private ParceToken lastToken;

    public Py4jEntryPoint() {
        eventBuffer = new ConcurrentLinkedQueue<>();
    }

    private Py4jParseEvent event;

    public void openEvent() {
        event = new Py4jParseEvent();
    }

    public void addContext(String name) {
        assert event != null;

        event.addContext(name);
    }

    public void addToken(int pos, String text, String action, String prefixWhitespace) {
        assert event != null;

        if (null != lastToken) {
            lastToken.setPostWhitespace(prefixWhitespace);
        }
        ParceToken lpt = new ParceToken(pos, text, action);
        event.addToken(lpt);
        lastToken = lpt;
    }

    public void closeEvent(int popContext) {
        assert event != null;

        event.setPopContext(popContext);
        if (!eventBuffer.offer(event)) {
            LOGGER.severe("could not add parse event to buffer");
        }
        event = null;
    }

    ConcurrentLinkedQueue<Py4jParseEvent> getBuffer() {
        return eventBuffer;
    }
}