package com.example;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.sprotty.ActionMessage;
import org.eclipse.sprotty.DefaultDiagramServer;
import org.eclipse.sprotty.Dimension;
import org.eclipse.sprotty.IDiagramExpansionListener;
import org.eclipse.sprotty.IDiagramOpenListener;
import org.eclipse.sprotty.IDiagramSelectionListener;
import org.eclipse.sprotty.IDiagramServer;
import org.eclipse.sprotty.ILayoutEngine;
import org.eclipse.sprotty.IModelUpdateListener;
import org.eclipse.sprotty.IPopupModelFactory;
import org.eclipse.sprotty.Point;
import org.eclipse.sprotty.SModelCloner;
import org.eclipse.sprotty.SModelRoot;
import org.eclipse.sprotty.SNode;
import org.eclipse.sprotty.server.json.ActionTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SpringBootApplication
@EnableWebSocket
@Controller
public class ChatterWebUiApplication implements WebSocketConfigurer, InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(ChatterWebUiApplication.class);
	
	private Set<WebSocketSession> ws_sessions = new HashSet<>();
	
	private Gson gson;
	
	@Autowired
	private ThreadPoolTaskScheduler scheduler;

	public static void main(String[] args) {
		SpringApplication.run(ChatterWebUiApplication.class, args);
	}
	
	protected void initializeGson() {
		if (gson == null) {
			GsonBuilder builder = new GsonBuilder();
			ActionTypeAdapter.configureGson(builder);
			gson = builder.create();
		}
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(WsMessageHandler(), "/websocket")
		.setAllowedOrigins("*")
		.withSockJS();
	}
	
	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}
	
	@Bean
	public ThreadPoolTaskScheduler scheduler() {
		ThreadPoolTaskScheduler t = new ThreadPoolTaskScheduler();
		t.initialize();
		return t;
	}
	
	@Bean
	public IDiagramServer diagramServer() {
		DefaultDiagramServer diagramServer = new DefaultDiagramServer("spring-boot");
//		diagramServer.setModel(newRoot);
		return diagramServer;
	}
	
	@Bean public IModelUpdateListener modelUpdateListener() {
		return new IModelUpdateListener.NullImpl();
	}
	
	@Bean public ILayoutEngine layoutEngine() {
		return new ILayoutEngine.NullImpl();
	}
	
	@Bean public IPopupModelFactory popupModelFactory() {
		return new IPopupModelFactory.NullImpl();
	}
	
	@Bean public IDiagramSelectionListener diagramSelectionListener() {
		return new IDiagramSelectionListener.NullImpl();
	}
	
	@Bean public IDiagramExpansionListener diagramExpansionListener() {
		return new IDiagramExpansionListener.NullImpl();
	}

	@Bean public IDiagramOpenListener diagramOpenListener() {
		return new IDiagramOpenListener.NullImpl();
	}
	
	@Bean public SModelCloner modelCloner() {
		return new SModelCloner();
	}
	
	/**
	 * WebSocketHandler which receives messages from a websocket and forwards them to a
	 * spring-cloud-stream.
	 */
	@Bean
	public WebSocketHandler WsMessageHandler() {
		return new TextWebSocketHandler() {

			@Override
			public void afterConnectionEstablished(WebSocketSession session) throws Exception {
				synchronized (ws_sessions) {
					ws_sessions.add(session);
				}
				log.info("Websocket connection OPENED in: "+this);
				log.info("Number of active sessions = {}", ws_sessions.size());
				diagramServer().setRemoteEndpoint(message -> {
					sendMessage(gson.toJson(message, ActionMessage.class));
				});
				
				scheduler.scheduleWithFixedDelay(() -> {
					diagramServer().setModel(generateModel());
				}, Duration.ofSeconds(2)); 
			}

			@Override
			protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
				log.info(message.getPayload());
				ActionMessage actionMessage = gson.fromJson(message.getPayload(), ActionMessage.class);
				diagramServer().accept(actionMessage);
			}

			@Override
			public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
				log.info("Websocket connection CLOSED in: "+this);
				synchronized (ws_sessions) {
					ws_sessions.remove(session);
				}
				log.info("Number of active sessions = {}", ws_sessions.size());
			}
			
			@Override
			public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
				log.error("Websocket trasnport error: ", exception);
				synchronized (ws_sessions) {
					ws_sessions.remove(session);
				}
			}
		};
	}
	
	private SModelRoot generateModel() {
		SModelRoot graph = new SModelRoot();
		graph.setId("graph");
		graph.setType("graph");
		graph.setChildren(new ArrayList<>()); 
		
		for (int i= 0; i < 1000; i++) {
			SNode node = new SNode();
		    node.setId("node11");
		    node.setType("node:circle");
		    node.setPosition(new Point(100 + i * 10, 100));
		    node.setSize(new Dimension(80, 80));
		    graph.getChildren().add(node);
		}
		
//
//	    let count = 2;
//	    function addNode(): SModelElementSchema[] {
//	        const newNode: SNodeSchema = {
//	            id: 'node' + count,
//	            type: 'node:circle',
//	            position: {
//	                x: Math.random() * 1024,
//	                y: Math.random() * 768
//	            },
//	            size: {
//	                width: 80,
//	                height: 80
//	            }
//	        };
//	        const newEdge: SEdgeSchema = {
//	            id: 'edge' + count,
//	            type: 'edge:straight',
//	            sourceId: 'node0',
//	            targetId: 'node' + count++
//	        };
//	        return [newNode, newEdge];
//	    }
//
//	    for (let i = 0; i < 200; ++i) {
//	        const newElements = addNode();
//	        for (const e of newElements) {
//	            graph.children.splice(0, 0, e);
//	        }
//	    }
		
		return graph;
	}
	
	public void sendMessage(@Payload String msg) {
		synchronized (ws_sessions) {
			for (WebSocketSession ws : ws_sessions) {
				try {
					if (ws.isOpen()) {
						log.info("Sent: {}", msg);
						ws.sendMessage(new TextMessage(msg));
					}
				} catch (Exception e) {
					log.error("Error forwarding message to ws session", e);
				}
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initializeGson();
	}
}
