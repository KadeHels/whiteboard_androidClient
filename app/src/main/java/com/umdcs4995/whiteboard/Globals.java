package com.umdcs4995.whiteboard;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.util.SortedList;

import com.umdcs4995.whiteboard.drawing.DrawingEventQueue;
import com.umdcs4995.whiteboard.protocol.WhiteboardProtocol;
import com.umdcs4995.whiteboard.services.SocketService;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Singleton class created for holding objects global to the application.
 * Created by rob on 2/26/16.
 */
public class Globals {

    //Instance for the global variables.
    private static Globals instance = null;

    private Context context;

    //The drawing event queue for the current whiteboard.
    private DrawingEventQueue drawingEventQueue;
    private WhiteboardProtocol whiteboardProtocol;

    //Socket service stuff
    private boolean socketServiceRunning = false;
    Intent socketServiceIntent;
    private SocketService socketService;
    private boolean isSocketServiceBound = false;

    /**
     * Private data member which binds the xmpp service to the client.
     */
    private ServiceConnection socketConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SocketService.SocketLocalBinder binder = (SocketService.SocketLocalBinder) service;
            socketService = binder.getService();
            isSocketServiceBound = true;

            //At this point the service is bound.
            socketService.setProtocol(whiteboardProtocol);
            whiteboardProtocol.setSocketService(socketService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isSocketServiceBound = false;
        }
    };


    //Hidden constructor.
    private Globals(Context applicationContext) {
        //Setup the priortiy measurements for the queue.
        this.context = applicationContext;
        socketServiceIntent = new Intent(context, SocketService.class);
        whiteboardProtocol = new WhiteboardProtocol();
    }

    /**
     * Called to initiate the singleton instance.  Note this needs to be called prior to the
     * getInstance method otherwise the app will blow up.
     * @return
     */
    public static void init(Context applicationContext) {
        if(instance == null) {
            instance = new Globals(applicationContext);
        }
    }

    /**
     * Returns the singleton instance.
     * @return an instance of Globals.
     */
    public static Globals getInstance(){
        if(instance == null) {
            return null;
        }
        return instance;
    }

    /**
     * Get the Whiteboard draw event queue.
     * @return
     */
    public DrawingEventQueue getDrawEventQueue(){
        return this.drawingEventQueue;
    }

    /**
     * Sets the DrawingEventQueue
     */
    public void setDrawEventQueue(DrawingEventQueue deq) {
        drawingEventQueue = deq;
    }

    /**
     * This method starts the socket service.
     */
    public void startSocketService() {
        if(socketServiceRunning) {
            //Safest thing to do here is to restart it.
            stopSocketService();
            startSocketService();
        } else {
            context.startService(socketServiceIntent);
            context.bindService(socketServiceIntent, socketConnection, Context.BIND_AUTO_CREATE);
            socketServiceRunning = true;
        }
    }

    /**
     * Stops (and destroys) the socket service.
     */
    public void stopSocketService() {
        if(socketServiceRunning) {
            context.stopService(socketServiceIntent);
            context.unbindService(socketConnection);
            socketServiceRunning = false;
        }
    }

    /**
     * Returns a reference to the socket service.
     */
    public SocketService getSocketService() {
        return socketService;
    }

    /**
     * Gets the application context
     */
    public Context getGlobalContext() {
        return context;
    }

    /**
     * Gets the Whiteboard protocol.
     * @return the Whiteboard protocol
     */
    public WhiteboardProtocol getWhiteboardProtocol() {
        return whiteboardProtocol;
    }

}
