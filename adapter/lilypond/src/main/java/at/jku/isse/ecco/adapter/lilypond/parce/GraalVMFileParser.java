package at.jku.isse.ecco.adapter.lilypond.parce;

import at.jku.isse.ecco.adapter.lilypond.LilypondNode;
import at.jku.isse.ecco.adapter.lilypond.LilypondParser;
import at.jku.isse.ecco.adapter.lilypond.LilypondPlugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraalVMFileParser implements LilypondParser<ParceToken> {
    protected static final Logger LOGGER = Logger.getLogger(LilypondPlugin.class.getName());
    private Context context;

    public void init() throws IOException {
        String VENV_EXECUTABLE = "/Users/rudolfhanl/Documents/graalpython/bin/graalpython";
        context = Context.newBuilder("python")
                .allowIO(true)
                .allowExperimentalOptions(true)
                .allowHostAccess(HostAccess.SCOPED)
                .option("python.Executable", VENV_EXECUTABLE)
                .option("python.ForceImportSite", "true")
                .build();
        context.eval("python", "import sys;import parce;" +
                "from parce.lang.lilypond import LilyPond;");
    }

    public LilypondNode<ParceToken> parse(Path path) {
        return parse(path, null);
    }

    public LilypondNode<ParceToken> parse(Path path, HashMap<String, Integer> tokenMetric) {
        LOGGER.log(Level.INFO, "start parsing {0}", path);

        GraalVMService service = new GraalVMService();
        context.getBindings("python").putMember("service", service);

        context.eval("python", "f = open('" + path.toString() + "', 'r', -1, 'UTF-8');" +
                "s = f.read();" +
                "f.close();" +
                "lastPos = 0;");

        context.eval("python", "for e in parce.events(LilyPond.root, 'g4 \\mf'):\n" +
                "    print(e)");

        context.eval("python", "for e in parce.events(LilyPond.root, s):\n" +
            "    pop = 0\n" +
            "    if e.target:\n" +
            "        pop = e.target.pop\n" +
            "        for c in e.target.push:\n" +
            "            service.addContext(c.fullname)\n" +
            "    print(e.lexemes)\n" +
            "    for tpl in e.lexemes:\n" +
            "        ws = s[lastPos:tpl[0]]\n" +
            "        service.addToken(tpl[0], tpl[1], str(tpl[2]), ws)\n" +
            "        lastPos = tpl[0] + len(tpl[1])\n" +
            "    service.closeEvent(pop)");

        return convertEventsToNodes(service.getEvents(), tokenMetric);
    }

    /**
     * Converts the token stream from Parce to a list of nodes.
     *
     * @param events List of parsing events.
     * @param tokenMetric Map to be filled with token metric if not null.
     * @return First node of list.
     */
    private LilypondNode<ParceToken> convertEventsToNodes(List<GraalVMService.ParseEvent> events, HashMap<String, Integer> tokenMetric) {
        LOGGER.log(Level.INFO, "convert {0} events to tree", events.size());
        int depth = 0, maxDepth = 0, cnt = 0;

        LilypondNode<ParceToken> head = new LilypondNode<>("HEAD", null);
        head.setLevel(depth);
        LilypondNode<ParceToken> n = head;

        Iterator<GraalVMService.ParseEvent> it = events.iterator();
        while (it.hasNext()) {
            GraalVMService.ParseEvent e = it.next();
            int pop = e.getPopContext();
            while (pop < 0) {
                pop++;
                depth--;

                LOGGER.log(Level.FINER, "closed context (depth: {0})", depth);
            }

            for (String s : e.getContexts()) {
                n = n.append(s, null, depth++);
                cnt++;
                LOGGER.log(Level.FINER, "opened {0}", n.getName());

                maxDepth = Math.max(depth, maxDepth);
            }

            for (ParceToken t : e.getTokens()) {
                if (null != tokenMetric) {
                    tokenMetric.put(t.getAction(), tokenMetric.getOrDefault(t.getAction(), 0) + 1);
                }

                n = n.append(t.getAction(), t, depth);
                cnt++;
                LOGGER.log(Level.FINER, "added token node '{0}' (depth: {1}), post '{2}'", new Object[] { t.getText(), depth, t.getPostWhitespace() });
            }
        }

        LOGGER.log(Level.INFO, "done conversion to {0} nodes (maxdepth: {1})", new Object[] { cnt, maxDepth });

        return head.getNext();
    }

    public void shutdown() {
        context.close();
    }

}
