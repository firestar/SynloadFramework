package com.synload.eventsystem;

import com.synload.eventsystem.events.RequestEvent;
import com.synload.eventsystem.events.WebEvent;

public class EventPublisher {
    public static void raiseEvent(final EventClass event, boolean threaded,
            final String target) {
        if (threaded) {
            new Thread() {
                @Override
                public void run() {
                    raise(event, target);
                }
            }.start();
        } else {
            raise(event, target);
        }
    }

    public static void raiseEvent(final EventClass event, String target) {
        raise(event, target);
    }

    private static void raise(final EventClass event, String target) {
        System.out.println("event class "+event.getClass().getName());
        System.out.println("check "+event.getHandler().getAnnotationClass().getName());
        if (HandlerRegistry.getHandlers().containsKey(event.getHandler().getAnnotationClass())) {
            //System.out.println("found "+event.getHandler().getAnnotationClass().getName());
            for (EventTrigger trigger : HandlerRegistry.getHandlers(event.getHandler().getAnnotationClass())){
                //System.out.println("check trigger "+trigger.getTrigger());
                if (event instanceof RequestEvent){
                    // Websocket Event!
                    //System.out.println("Comparing");
                    if (trigger.getTrigger().length == 2) {
                        RequestEvent requestEvent = (RequestEvent) event;
                        //System.out.println(trigger.getTrigger()[0]+" to method "+requestEvent.getRequest().getMethod());
                        //System.out.println(trigger.getTrigger()[1]+" to action "+requestEvent.getRequest().getAction());
                        if (
                                trigger.getTrigger()[0].equalsIgnoreCase(requestEvent.getRequest().getMethod()) // method / method
                                && trigger.getTrigger()[1].equalsIgnoreCase(requestEvent.getRequest().getAction()) // action / action
                        ) {
                            try {
                                trigger.getMethod().invoke(trigger.getHostClass().newInstance(), requestEvent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }
                } else { // No more WebEvent, handled in HttpRouting, Custom Events Only
                    if (trigger.getMethod().getParameterTypes().length>0 && trigger.getMethod().getParameterTypes()[0].isInstance(event)) {
                        try {
                            trigger.getMethod().invoke(trigger.getHostClass().newInstance(), event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}