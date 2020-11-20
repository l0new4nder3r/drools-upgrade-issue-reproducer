package me.nbuchon;

import java.util.ArrayList;
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
      // init ksession from reproducer.drl
        final KieServices ks = KieServices.Factory.get();
        final KieContainer kc = ks.getKieClasspathContainer();
        final KieSession ksession = kc.newKieSession("ReproducerKS");
        ksession.setGlobal( "list",
                            new ArrayList<Object>() );
        ksession.addEventListener( new DebugAgendaEventListener() );
        ksession.addEventListener( new DebugRuleRuntimeEventListener() );

        // To setup a file based audit logger, uncomment the next line
        // KieRuntimeLogger logger = ks.getLoggers().newFileLogger( ksession, "./helloworld" );

        // To setup a ThreadedFileLogger, so that the audit view reflects events whilst debugging,
        // uncomment the next line
        // KieRuntimeLogger logger = ks.getLoggers().newThreadedFileLogger( ksession, "./helloworld", 1000 );

        // The application can insert facts into the session
        final Message message = new Message();
        message.setMessage( "Hello World" );
        message.setStatus( Message.HELLO );
        ksession.insert( message );

        final List<Thread> threads = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
          final Wrapper<Integer> cpt = new Wrapper<>(i);
          // preparing 100 successive query calls in another thread
          final Thread threadedQueryCalls = new Thread() {
            @Override
            public void run() {
              int foundCount = 0;
              for (int j = 0; j < 100; j++) {
                System.out.println("Call query Thread n°" + cpt.get() + " n°" + (j + 1));
                if (BasicDroolsRule.callQuery(ksession)) {
                  foundCount++;
                }
              }
              System.out.println("End of queryCalls, found message " + foundCount + " times.");
            }
          };
          threads.add(threadedQueryCalls);
        }

        for (final Thread thread : threads) {
          // starting those calls
          thread.start();
        }

        // and fire the rules in the meantime!
        ksession.fireAllRules();

        BasicDroolsRule.callQuery(ksession);

        // Remove comment if using logging
        // logger.close();

        // and then dispose the session
        ksession.dispose();
    }

    private static boolean callQuery(final KieSession ksession) {
      final Wrapper<Boolean> found = new Wrapper<Boolean>(false);
      ksession.getQueryResults("message here").forEach(result -> {
        final Message here = (Message) result.get("$here");
        System.out.println("Found a message here: " + here.getMessage());
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
                                                 Thread.sleep(200);
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
