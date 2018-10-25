package examples.primeira_entrega;

import java.awt.Rectangle;
import java.net.UnknownHostException;

import simple_soccer_lib.PlayerCommander;
import simple_soccer_lib.perception.FieldPerception;
import simple_soccer_lib.perception.MatchPerception;
import simple_soccer_lib.perception.PlayerPerception;
import simple_soccer_lib.utils.EFieldSide;
import simple_soccer_lib.utils.Vector2D;


public class Acao_do_goleiro extends Thread {
	private PlayerCommander commander;
	
	private PlayerPerception selfPerc;
	private FieldPerception  fieldPerc;
	
	private int LOOP_INTERVAL = 100;
	long nextIteration = System.currentTimeMillis() + LOOP_INTERVAL;
	
	public Acao_do_goleiro() throws UnknownHostException {
		commander = new PlayerCommander("RED", "localhost", 6000, false);		
	}
	
	public Acao_do_goleiro(PlayerCommander player) {
		commander = player;
	}

	@Override
	public void run() {
		System.out.println(">> 1. Waiting initial perceptions...");
		selfPerc  = commander.perceiveSelfBlocking();
		fieldPerc = commander.perceiveFieldBlocking();
		
		System.out.println(">> 2. Moving to initial position...");
		commander.doMoveBlocking(-25.0d, 0.0d);
		
		selfPerc  = commander.perceiveSelfBlocking();
		fieldPerc = commander.perceiveFieldBlocking();
		
		System.out.println(">> 3. Now starting...");
		while (commander.isActive()) {
			
			acaoGoleiro(nextIteration);
		}
		
		System.out.println(">> 4. Terminated!");
	}

	private void kickToPoint(double x, double y){
		Vector2D myPos = selfPerc.getPosition();
		Vector2D point = new Vector2D(x, y);
		Vector2D newDirection = point.sub(myPos);
		
		
		commander.doTurnToDirectionBlocking(newDirection);
		
		double intensity = (newDirection.magnitude() * 100) / 40;
		if(intensity > 100){
			intensity = 100;
		}
		commander.doKickBlocking(intensity, 0);
	}
	
	private void updatePerceptions() {
		PlayerPerception newSelf = commander.perceiveSelf();
		FieldPerception newField = commander.perceiveField();
		
		if (newSelf != null) {
			this.selfPerc = newSelf;
		}
		if (newField != null) {
			this.fieldPerc = newField;
		}
	}
	

	private void turnToPoint(Vector2D point){
		Vector2D newDirection = point.sub(selfPerc.getPosition());
		commander.doTurnToDirectionBlocking(newDirection);
	}
	
	
	private boolean isPointsAreClose(Vector2D reference, Vector2D point, double margin){
		return reference.distanceTo(point) <= margin;
	}
	
	private void dash(Vector2D point){
		if(selfPerc.getPosition().distanceTo(point)<=1) return;
	}
	
	private void acaoGoleiro(long nextInteration){
		double xInit = -48, yInit=0, ballX= 0, ballY=0;
		EFieldSide side = selfPerc.getSide();
		Vector2D initPos = 
				new Vector2D(xInit*side.value(), yInit*side.value());
		Vector2D ballPos;
		Rectangle area = side == EFieldSide.LEFT?
				new Rectangle(-52,-20,16,40):
				new Rectangle(36,-20,16,40);
		while(true){
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();
			MatchPerception matchPerc = new MatchPerception(); 
			
			switch(matchPerc.getState()){
			case BEFORE_KICK_OFF:
				commander.doMoveBlocking(xInit, yInit);
				break;
			case PLAY_ON:
				ballX = fieldPerc.getBall().getPosition().getX();
				ballY = fieldPerc.getBall().getPosition().getY();
				if(isPointsAreClose(selfPerc.getPosition(),ballPos, 1)){
					//DUVIDA NO VALOR 0.0 POIS ERA NEW VECTOR2D(0,0) NO LOCAL DO 0.0
					kickToPoint( 0.0, 100);
				}else if(area.contains(ballX, ballY)){
					dash(ballPos);
				}else if(!isPointsAreClose(selfPerc.getPosition(),initPos, 3)){
					
				dash(initPos);
				}else{
					turnToPoint(ballPos);
				}
				break;
				
				
				default: break;
			}
		}
	}

}
