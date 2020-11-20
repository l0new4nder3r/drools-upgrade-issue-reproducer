package me.nbuchon;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.kie.api.KieServices;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;


/**
 * This is a sample file to launch a rule package from a rule source file. Copied from HelloWorld official
 * drools-examples and trial-error adapted to try and reproduce an issue we face in migrating from drools 6.2 to 7.x.
 */
public class BasicDroolsRule {

    public static final void main(final String[] args) {
      // Initialising 100 sessions
      final List<KieSession> sessions = new ArrayList<>(20);
      for (int i = 0; i < 20; i++) {
        // init ksession from reproducer.drl
        final KieServices ks = KieServices.Factory.get();
        final KieContainer kc = ks.getKieClasspathContainer();
        final KieSession ksession = kc.newKieSession("ReproducerKS");
        ksession.setGlobal( "list",
                            new ArrayList<Object>() );
        ksession.addEventListener( new DebugAgendaEventListener() );
        ksession.addEventListener( new DebugRuleRuntimeEventListener() );
        sessions.add(ksession);
      }

      // Init 100 threads to call a query
        final List<Thread> threads = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
          final Wrapper<Integer> cpt = new Wrapper<>(i);
          // preparing 100 successive query calls per thread
          final Thread threadedQueryCalls = new Thread() {
            @Override
            public void run() {
              int foundCount = 0;
              for (int j = 0; j < 1000; j++) {
                System.out.println("Calling all the sessions' query by Thread n°" + cpt.get() + " n°" + (j + 1));
                for (final KieSession session : sessions) {
                  if (BasicDroolsRule.callQueries(session)) {
                    foundCount++;
                  }
                }
              }
              System.out.println("End of queryCalls for thread n°" + cpt.get() + ", found message here " + foundCount + " times.");
            }
          };
          threads.add(threadedQueryCalls);
        }

        // start those threads
        for (final Thread thread : threads) {
          // starting those calls
          thread.start();
        }

        for (final KieSession session : sessions) {
          // let's insert messages and fire all the rules, 100 times
          for (int i = 0; i < 100; i++) {
            final Message message = new Message();
            message.setMessage("Hello World");
            message.setStatus(Message.HELLO);
            session.insert(message);

            // and fire the rules in the meantime!
            session.fireAllRules();

            BasicDroolsRule.callQueries(session);
          }
        }

        // and then dispose the session
        for (final KieSession session : sessions) {
          session.dispose();
        }
    }

    private static boolean callQueries(final KieSession ksession) {
      final Wrapper<Boolean> found = new Wrapper<Boolean>(false);
      ksession.getQueryResults("message here").forEach(result -> {
        final Message here = (Message) result.get("$here");
        System.out.println("Found a message here: " + here.getMessage());
        final FoundMessage last = (FoundMessage) result.get("$last");
        System.out.println("Found, last message: " + last.getDatetime());
        found.set(true);
      });
      ksession.getQueryResults("count messages here").forEach(result -> {
        final Long count = (Long) result.get("$messagesCount");
        // System.out.println("Found count messages : " + count);
        found.set(true);
      });
      ksession.getQueryResults("hi message").forEach(result -> {
        final Message hi = (Message) result.get("$hello");
        System.out.println("Found a hi message: " + hi.getMessage());
        found.set(true);
      });
      return found.get();
    }

    public static class Message {
        public static final int HELLO   = 0;
        public static final int HERE = 1;
        public static final int GOODBYE = 2;

        private String          message;

        private int             status;
        private final Callable<Boolean> test = new Callable<Boolean>() {

                                               @Override
                                               public Boolean call() throws Exception {
                                                 System.out.println("dodo");
                                                 Thread.sleep(100);
                                                 return true;
                                               }
                                             };

        public Message() {

        }

        public String getMessage() {
            return this.message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }

        public int getStatus() {
            return this.status;
        }

        public void setStatus(final int status) {
            this.status = status;
        }

        public static Message doSomething(final Message message) {
            return message;
        }

        public boolean isSomething(final String msg,
                                   final List<Object> list) {
            list.add( this );
            return this.message.equals( msg );
        }

        public Callable<Boolean> getTest() {
          return this.test;
        }
    }

    public static class FoundMessage {
      private Date datetime;
      private String message;
      private int  status;

      public FoundMessage(final String message, final int status, final Date date) {
        this.datetime = date;
        this.message = message;
        this.status = status;
      }

      public Date getDatetime() {
        return this.datetime;
      }

      public void setDatetime(final Date datetime) {
        this.datetime = datetime;
      }

      public String getMessage() {
        return this.message;
      }

      public void setMessage(final String message) {
        this.message = message;
      }

      public int getStatus() {
        return this.status;
      }

      public void setStatus(final int status) {
        this.status = status;
      }

    }

    public static class Wrapper<T> {

      public Wrapper(final T val) {
        super();
        this.val = val;
      }

      private T val;

      /**
       * @return the val
       */
      public T get() {
        return this.val;
      }

      /**
       * @param val the val to set
       */
      public void set(final T val) {
        this.val = val;
      }

    }
}
