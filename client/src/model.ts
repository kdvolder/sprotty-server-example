import {ManhattanEdgeRouter, RectangularNode, SEdge, WebSocketDiagramServer, ActionMessage} from "sprotty";
import {injectable} from 'inversify';

export class BeanNode extends RectangularNode {

}

export class OrthogonalEgde extends SEdge {

    routerKind = ManhattanEdgeRouter.KIND;

}

const SOCKET_MESSAGE_BUFFER = 4000;
const END_MESSAGE = '@end';

@injectable()
export class ExampleWebsocketDiagramServer extends WebSocketDiagramServer {

    protected sendMessage(message: ActionMessage): void {
        if (this.webSocket) {
            const messageStr = JSON.stringify(message);
            let offset = 0;
            while (offset < messageStr.length) {
                this.webSocket.send(messageStr.substring(offset, offset + SOCKET_MESSAGE_BUFFER));
                offset += SOCKET_MESSAGE_BUFFER;
            }
            this.webSocket.send(END_MESSAGE);
        } else {
            throw new Error('WebSocket is not connected');
        }
    }
}
