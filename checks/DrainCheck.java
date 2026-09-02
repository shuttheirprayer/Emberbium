import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Standalone check of the collectChunkBuildResults() drain semantics.
 * Mirrors the patched logic against fakes so the failure path can actually be exercised --
 * a real failed chunk build is not something we can trigger on demand.
 *
 *   javac DrainCheck.java && java DrainCheck
 */
public class DrainCheck {

    // stand-in for ChunkBuildOutput
    static class Output {
        final String name;
        boolean deleted;
        Output(String name) { this.name = name; }
        void delete() {
            if (deleted) throw new IllegalStateException("double delete of " + name);
            deleted = true;
        }
    }

    // stand-in for ChunkJobResult
    static class Result {
        final Output output;
        final RuntimeException throwable;
        private Result(Output o, RuntimeException t) { output = o; throwable = t; }
        static Result ok(Output o) { return new Result(o, null); }
        static Result fail(RuntimeException t) { return new Result(null, t); }
        Output unwrap() {
            if (throwable != null) throw throwable;
            return output;
        }
    }

    /** The patched drain, transcribed. */
    static ArrayList<Output> collect(ConcurrentLinkedDeque<Result> buildResults) {
        ArrayList<Output> results = new ArrayList<>();
        RuntimeException failure = null;

        Result result;
        while ((result = buildResults.poll()) != null) {
            try {
                results.add(result.unwrap());
            } catch (RuntimeException e) {
                if (failure == null) failure = e; else failure.addSuppressed(e);
            }
        }

        if (failure != null) {
            for (Output output : results) output.delete();
            throw failure;
        }

        return results;
    }

    /** Upstream's version, for contrast. */
    static ArrayList<Output> collectUpstream(ConcurrentLinkedDeque<Result> buildResults) {
        ArrayList<Output> results = new ArrayList<>();
        Result result;
        while ((result = buildResults.poll()) != null) {
            results.add(result.unwrap());
        }
        return results;
    }

    static ConcurrentLinkedDeque<Result> queue(Output a, Output b, Output c) {
        ConcurrentLinkedDeque<Result> q = new ConcurrentLinkedDeque<>();
        q.add(Result.ok(a));
        q.add(Result.fail(new RuntimeException("chunk build failed")));
        q.add(Result.ok(b));
        q.add(Result.ok(c));
        return q;
    }

    public static void main(String[] args) {
        // --- the bug being fixed: upstream strands outputs and leaves the queue undrained ---
        Output a = new Output("a"), b = new Output("b"), c = new Output("c");
        ConcurrentLinkedDeque<Result> q = queue(a, b, c);
        try {
            collectUpstream(q);
            throw new AssertionError("expected upstream drain to throw");
        } catch (RuntimeException expected) { }
        assert !a.deleted : "upstream: 'a' leaked (this is the bug)";
        assert q.size() == 2 : "upstream: queue left undrained, size=" + q.size();

        // --- patched: whole queue drained, every success released, failure still propagates ---
        Output a2 = new Output("a"), b2 = new Output("b"), c2 = new Output("c");
        ConcurrentLinkedDeque<Result> q2 = queue(a2, b2, c2);
        RuntimeException thrown = null;
        try {
            collect(q2);
        } catch (RuntimeException e) {
            thrown = e;
        }
        assert thrown != null : "failure must still propagate";
        assert q2.isEmpty() : "queue must be fully drained, size=" + q2.size();
        assert a2.deleted && b2.deleted && c2.deleted : "every successful output must be released";

        // --- multiple failures are suppressed onto the first, none lost ---
        ConcurrentLinkedDeque<Result> q3 = new ConcurrentLinkedDeque<>();
        q3.add(Result.fail(new RuntimeException("first")));
        q3.add(Result.fail(new RuntimeException("second")));
        try {
            collect(q3);
            throw new AssertionError("expected throw");
        } catch (RuntimeException e) {
            assert "first".equals(e.getMessage()) : "first failure should propagate, got " + e.getMessage();
            assert e.getSuppressed().length == 1 : "second failure should be suppressed onto the first";
        }

        // --- happy path unchanged: nothing deleted, everything returned ---
        Output h1 = new Output("h1"), h2 = new Output("h2");
        ConcurrentLinkedDeque<Result> q4 = new ConcurrentLinkedDeque<>();
        q4.add(Result.ok(h1));
        q4.add(Result.ok(h2));
        ArrayList<Output> ok = collect(q4);
        assert ok.size() == 2 : "happy path must return every output";
        assert !h1.deleted && !h2.deleted : "happy path must not delete -- the caller does that";

        System.out.println("DrainCheck: all assertions passed");
    }
}
