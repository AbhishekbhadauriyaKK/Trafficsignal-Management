import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Car {
	protected static int carCount = 0;
	protected static int mCarCount = 0;		//car is moving through grid
	protected static int sCarCount = 0;		//car in statistics phase
	private static HashMap <char[], Car> allCars = new HashMap<char[], Car>();
	
	//Convoy pointer
	Convoy convoy = null;
	
	protected int[] xy = new int[]{-1, -1};	//x, y position in grid
	protected TrafficPoint entrancePoint;
	protected TrafficPoint exitPoint;
	protected TrafficPoint turningPoint1 = null;			//TBD intersection ID only or whole object
	protected TrafficPoint turningPoint2 = null;			//if whole object then function equals to be implemented
	protected TrafficPoint nextPoint = null;
	protected int[] dxy = new int[2];						//horizontal and vertical speed
	protected Road road;
	protected char lane = 'M';
	/*
	 * M=middle, L=left, R=right
	 */
	protected int remainingTurns = 0;
	protected char dir;
	protected char[] carID = new char[8];
	/*
	 * char[0]=entrance direction
	 * char[1]=exit direction
	 * char[2]=number of turns
	 * char[3:7]=unique ID
	 */
	//Code added here
	public long entryTime;
	public long exitTime;
	public long queueTime;
	public long carDistance;
	//String tempStringExit;
	//Code added here
	
	//protected boolean isQueued = false;
	//protected boolean inAQueue = false;
	//protected Car nextInLine = null;
	//true=car is attached to a queue, false=car running in grid
	protected char phase = 'Q';
	/*
	 * Q=queue or before, M=moving, S=statistics or after
	 */
	private Car(int ID, TrafficPoint entrance, TrafficPoint exit) {
		this.carID = Arrays.copyOfRange(getCarIDFromInt(ID), 0, 8);
		this.carID[0] = entrance.roadDir[0];	//Direction same for entrance
		this.carID[1] = exit.roadDir[0];		//Direction same for exit
		if(carID[0] == carID[1])		//for the time being
			carID[2] = '0';
		else
			carID[2] = '0';
		this.entrancePoint = entrance;
		this.exitPoint = exit;
	}
	
	public static void addCars(int numberOfCars) {
		Car tempCar;
		//TrafficPoint entrance, exit;
		char[] roadID;
		char intermediateDirection = 'N';
		boolean again = true;
		Road tempRoad1, tempRoad2, tempRoad3;
		TrafficPoint entrance, exit, turningPoint;
		Object[] roadKeysRand = Road.getKeySet().toArray();
		int i=0;
		int rand = 0;
		while(i++<numberOfCars) {
			//random pick of entrance road
			roadID = (char[]) roadKeysRand[new Random().nextInt(roadKeysRand.length)];
			tempRoad1 = Road.getRoad(roadID);
			//if(tempRoad1.roadDir != 'N') continue;
			//random pick of {fit} exit road
			do {
				roadID = (char[]) roadKeysRand[new Random().nextInt(roadKeysRand.length)];
				tempRoad2 = Road.getRoad(roadID);
				if((tempRoad1.roadDir == 'N' && tempRoad2.roadDir == 'S') || 
						(tempRoad1.roadDir == 'S' && tempRoad2.roadDir == 'N') ||
						(tempRoad1.roadDir == 'E' && tempRoad2.roadDir == 'W') ||
						(tempRoad1.roadDir == 'W' && tempRoad2.roadDir == 'E'))
					again = true;
				else
					again = false;
			} while(again);
			entrance = tempRoad1.entrancePoint;
			exit = tempRoad2.exitPoint;
			tempCar = new Car(++carCount, entrance, exit);
			tempCar.road = (entrance.street == null)? entrance.avenue : entrance.street;
			//determine number of turns and turning points
			tempCar.remainingTurns = 0;
			if(tempRoad1 != tempRoad2) {
				if((tempRoad1.roadDir == 'N' || tempRoad1.roadDir == 'S')  &&
						(tempRoad2.roadDir == 'E' || tempRoad2.roadDir == 'W')) {
					//street to avenue
					tempCar.remainingTurns++;
					turningPoint = entrance.nextAvenue;
					again = true;
					do {
						//System.out.println(turningPoint.pointID);
						if(turningPoint.street == tempRoad2) {
							tempCar.turningPoint1 = turningPoint;
							again = false;
						} else {
							if(turningPoint.control[1] == 'X') {
								System.out.println("Reached an EXIT Error");
							}
							turningPoint = turningPoint.nextAvenue;
							//again = true;
						}
					} while(again);
				} else if((tempRoad1.roadDir == 'E' || tempRoad1.roadDir == 'W')  &&
						(tempRoad2.roadDir == 'N' || tempRoad2.roadDir == 'S')) {
					//avenue to street
					tempCar.remainingTurns++;
					turningPoint = entrance.nextStreet;
					again = true;
					do {
						//System.out.println(turningPoint.pointID);
						if(turningPoint.avenue == tempRoad2) {
							tempCar.turningPoint1 = turningPoint;
							again = false;
						} else {
							if(turningPoint.control[1] == 'X') {
								System.out.println("Reached an EXIT Error");
							}
							turningPoint = turningPoint.nextStreet;
							//again = true;
						}
					} while(again);
				} else {
					//two turns
					tempCar.remainingTurns = 2;
					//determine the intermediate turning direction
					if(tempRoad1.sectors[1] < tempRoad2.sectors[1]) {
						if(tempRoad1.roadDir == 'N')
							intermediateDirection = 'E';
						else if(tempRoad1.roadDir == 'S')
							intermediateDirection = 'W';
						else if(tempRoad1.roadDir == 'E')
							intermediateDirection = 'S';
						else if(tempRoad1.roadDir == 'W')
							intermediateDirection = 'N';
					} else {
						if(tempRoad1.roadDir == 'N')
							intermediateDirection = 'W';
						else if(tempRoad1.roadDir == 'S')
							intermediateDirection = 'E';
						else if(tempRoad1.roadDir == 'E')
							intermediateDirection = 'N';
						else if(tempRoad1.roadDir == 'W')
							intermediateDirection = 'S';
					}
					/*System.out.println("2T ENT DIR="+tempRoad1.roadDir+"\tEXT DIR="
							+tempRoad2.roadDir+"\tFROM "+tempRoad1.sectors[1]+"\tTO "
							+tempRoad2.sectors[1]+"\tINT DIR="+intermediateDirection);*/
					//determining intermediate road direction
					again = true;
					do { 
						roadID = (char[]) roadKeysRand[new Random().nextInt(roadKeysRand.length)];
						tempRoad3 = Road.getRoad(roadID);
						if(tempRoad3.roadDir == intermediateDirection)
							again = false;
					} while(again);
					//System.out.println(tempRoad3.roadID);
					turningPoint = tempRoad3.entrancePoint;
					//street to avenue to street
					if(tempRoad3.roadType == 'A') {		//intermediate is avenue
						//searching for turning point 1
						again = true;
						do {
							//System.out.println(turningPoint.pointID);
							if(turningPoint.street == tempRoad1) {
								tempCar.turningPoint1 = turningPoint;
								again = false;
							} else {
								if(turningPoint.control[1] == 'X') {
									System.out.println("Reached an EXIT Error");
								}
								turningPoint = turningPoint.nextAvenue;
								//again = true;
							}
						} while(again);
						//searching for turning point 2
						again = true;
						do {
							//System.out.println(turningPoint.pointID);
							if(turningPoint.street == tempRoad2) {
								tempCar.turningPoint2 = turningPoint;
								again = false;
							} else {
								if(turningPoint.control[1] == 'X') {
									System.out.println("Reached an EXIT Error");
								}
								turningPoint = turningPoint.nextAvenue;
								//again = true;
							}
						} while(again);
					}
					//avenue to street to avenue
					else if(tempRoad3.roadType == 'S') {
						//searching for turning point 1
						again = true;
						do {
							//System.out.println(turningPoint.pointID);
							if(turningPoint.avenue == tempRoad1) {
								tempCar.turningPoint1 = turningPoint;
								again = false;
							} else {
								if(turningPoint.control[1] == 'X') {
									System.out.println("Reached an EXIT Error");
								}
								turningPoint = turningPoint.nextStreet;
								//again = true;
							}
						} while(again);
						//searching for turning point 2
						again = true;
						do {
							//System.out.println(turningPoint.pointID);
							if(turningPoint.avenue == tempRoad2) {
								tempCar.turningPoint2 = turningPoint;
								again = false;
							} else {
								if(turningPoint.control[1] == 'X') {
									System.out.println("Reached an EXIT Error");
								}
								turningPoint = turningPoint.nextStreet;
								//again = true;
							}
						} while(again);
					}
				}
			}
			//determine entrance lane
			if(tempCar.remainingTurns == 0) {
				rand = (int)(Math.random()*3);
				if(rand == 0) tempCar.lane = 'L';
				else if(rand == 1) tempCar.lane = 'M';
				else if(rand == 2) tempCar.lane = 'R';
			} else {
				if(tempCar.road.roadDir == 'N') {
					if(tempCar.turningPoint1.roadDir[0] == 'E')
						tempCar.lane = 'R';
					else if(tempCar.turningPoint1.roadDir[0] == 'W')
						tempCar.lane = 'L';
				} else if(tempCar.road.roadDir == 'S') {
					if(tempCar.turningPoint1.roadDir[0] == 'E')
						tempCar.lane = 'L';
					else if(tempCar.turningPoint1.roadDir[0] == 'W')
						tempCar.lane = 'R';
				} else if(tempCar.road.roadDir == 'E') {
					if(tempCar.turningPoint1.roadDir[1] == 'N')
						tempCar.lane = 'L';
					else if(tempCar.turningPoint1.roadDir[1] == 'S')
						tempCar.lane = 'R';
				} else if(tempCar.road.roadDir == 'W') {
					if(tempCar.turningPoint1.roadDir[1] == 'N')
						tempCar.lane = 'R';
					else if(tempCar.turningPoint1.roadDir[1] == 'S')
						tempCar.lane = 'L';
				}
			}
			tempCar.dxy = new int[]{0, 0};
			allCars.put(tempCar.carID, tempCar);
			tempCar.queueTime = Frame.systemTime;
			if(entrance.roadDir[0] == 'N' || entrance.roadDir[0] == 'S')
				entrance.comingCars[1]++;
			else if(entrance.roadDir[0] == 'E' || entrance.roadDir[0] == 'W')
				entrance.comingCars[0]++;
		}
		//System.out.println(numberOfCars + " Cars Added to the Grid");
	}
	
	public static Set<Map.Entry<char[] ,Car>> getEntrySet() {
		return allCars.entrySet();
	}
	
	
	public boolean enterGrid() {
		if (phase != 'Q') return false;
		this.phase = 'M';
		this.entryTime = Frame.systemTime;
		return true;
	}
	
	private void increaseSpeed() {
		//speed changes for first car only and it change it for all
		if(Frame.schedulingScheme == 'V') {
			if(this.convoy != null && this.convoy.listOfCars[0] != this) {
				return;
			}
		}
		
		if(this.dir == 'N') {
			this.dxy[1] = Math.min(this.dxy[1]+Frame.carAcceleration, Frame.carSpeed);
			this.dxy[0] = 0;
		}
		else if(this.dir == 'S') {
			this.dxy[1] = Math.max(this.dxy[1]-Frame.carAcceleration, -1*Frame.carSpeed);
			this.dxy[0] = 0;
		}
		else if(this.dir == 'E') {
			this.dxy[0] = Math.max(this.dxy[0]-Frame.carAcceleration, -1*Frame.carSpeed);
			this.dxy[1] = 0;
		}
		else if(this.dir == 'W') {
			this.dxy[0] = Math.min(this.dxy[0]+Frame.carAcceleration, Frame.carSpeed);
			this.dxy[1] = 0;
		}
		//copy same speed for all cars in convoy
		if(this.convoy != null) this.convoy.changeSpeedForAll(this.dxy);
	}
	private void decreaseSpeed() {
		if(Frame.schedulingScheme == 'V') {
			if(this.convoy != null && this.convoy.listOfCars[0] != this) {
				return;
			}
		}
		
		if(this.dir == 'N') {
			this.dxy[1] = Math.max(this.dxy[1]-Frame.carAcceleration, 0);
			this.dxy[0] = 0;
		}
		else if(this.dir == 'S') {
			this.dxy[1] = Math.min(this.dxy[1]+Frame.carAcceleration, 0);
			this.dxy[0] = 0;
		}
		else if(this.dir == 'E') {
			this.dxy[0] = Math.min(this.dxy[0]+Frame.carAcceleration, 0);
			this.dxy[1] = 0;
		}
		else if(this.dir == 'W') {
			this.dxy[0] = Math.max(this.dxy[0]-Frame.carAcceleration, 0);
			this.dxy[1] = 0;
		}
		//copy same speed for all cars in convoy
		if(this.convoy != null) this.convoy.changeSpeedForAll(this.dxy);
	}
	
	public void switchSpeed() {
		if(this.convoy != null) this.convoy.leave();
		
		int temp = Math.abs(this.dxy[0]+this.dxy[1]);
		if(this.dir == 'N')
			this.dxy = new int[]{0, temp};
		else if(this.dir == 'S')
			this.dxy = new int[]{0, -1*temp};
		else if(this.dir == 'E')
			this.dxy = new int[]{-1*temp, 0};
		else if(this.dir == 'W')
			this.dxy = new int[]{temp, 0};
	}
	
	public void moveXY(int distance, Car inFront) {
		
		//car outside grid *was a nested under condition if next point is exit 
		if(this.xy[0] < 0 || this.xy[0] > Road.xAccumulativePosition ||
				this.xy[1] < 0 || this.xy[1] > Road.yAccumulativePosition) {
			Car.mCarCount--;
			Car.sCarCount++;
			this.phase = 'S';
			exitTime = Frame.systemTime;
			//if(this.convoy != null) this.convoy.leave(this);
			return;
		}
		
		if(Math.abs(this.nextPoint.distance(this)) <= Frame.fullDistance) {
			char tempDir = this.dir;
			boolean[] decide = this.nextPoint.intersectionLogic(this, this.dxy);
			if(decide[0]) {
				this.increaseSpeed();
				if(decide[1] && this.nextPoint.control[1] != 'X') {
					if(tempDir == 'N' || tempDir == 'S') {
						this.nextPoint.comingCars[1]--;		//subtract cars coming from avenue
						if(this.dir == 'E' || this.dir == 'W') {
							this.nextPoint.expectedTurningCars[0]--;	//car turned
						} else {
							this.nextPoint.expectedStraightCars[1]--;	//car in same direction
						}
					} else if(tempDir == 'E' || tempDir == 'W') {
						this.nextPoint.comingCars[0]--;		//subtract cars coming from street
						if(this.dir == 'N' || this.dir == 'S') {
							this.nextPoint.expectedTurningCars[1]--;
						} else {
							this.nextPoint.expectedStraightCars[0]--;
						}
					}
					/*System.out.print(this.carID);
					System.out.print("\t"+this.dir+"\t"+this.xy[0]+"\t"+this.xy[1]+"\t");
					System.out.print(this.nextPoint.pointID);*/
					if(this.dir == 'N' || this.dir == 'S') {
						this.nextPoint = this.nextPoint.nextAvenue;
						this.nextPoint.comingCars[1]++;
						//each car is incremented in the perspective counter for street or avenue
						if(this.nextPoint == this.turningPoint1 || this.nextPoint == this.turningPoint2)
							this.nextPoint.expectedTurningCars[1]++;
						else
							this.nextPoint.expectedStraightCars[1]++;		//add cars coming from avenue
					} else if(this.dir == 'E' || this.dir == 'W') {
						this.nextPoint = this.nextPoint.nextStreet;
						this.nextPoint.comingCars[0]++;
						if(this.nextPoint == this.turningPoint1 || this.nextPoint == this.turningPoint2)
							this.nextPoint.expectedTurningCars[0]++;
						else
							this.nextPoint.expectedStraightCars[0]++;
					}
					/*System.out.print("\t");
					System.out.println(this.nextPoint.pointID);*/
				}
			} else {
				//if(this.convoy != null) this.convoy.leave(this);
				this.decreaseSpeed();
			}
		} else if(distance <= Frame.fullDistance) {
			this.decreaseSpeed();
		} else this.increaseSpeed();
		
		if(Frame.schedulingScheme == 'V') {
			//join cars into convoys
			//convoys form only at stop
			if(Math.abs(this.dxy[0]+this.dxy[1]) == 0 && inFront != null && this.convoy == null) {
				if(distance < Frame.fullDistance && distance > Frame.carLength && 
						inFront.convoy != null) {
					this.convoy = inFront.convoy.joinConvoy(this);
				}
			}
			if(Math.abs(this.dxy[0]+this.dxy[1]) == 0 && this.convoy == null) 
				this.convoy = new Convoy(this);
			
			if((this.dxy[0]+this.dxy[1]) != 0 && this.convoy != null) {
				if(this.convoy.carsInConv == 1) this.convoy.leave();
			}
			//if convoy does not have head ==> kill it
			if(this.convoy != null && (this.convoy.listOfCars[0] == null || 
					this.convoy.listOfCars[0].convoy != this.convoy)) this.convoy.leave();
		}
		
		this.xy[0] += this.dxy[0];
		this.xy[1] += this.dxy[1];
		this.carDistance += (this.dxy[0] == 0)? Math.abs(this.dxy[1]):Math.abs(this.dxy[0]);
	}
	
	/*public boolean queueCar(Car nextCar) {
		//if(nextCar.inAQueue) return false;
		if(this == nextCar) {
			//System.out.println("Same car queuing itself");
			return false;
			//System.exit(0);
		}
		if(this.isQueued)			//continue till last one and queue to it
			return this.nextInLine.queueCar(nextCar);
		else {
			this.nextInLine = nextCar;
			//nextCar.inAQueue = true;
			isQueued = true;
			return isQueued;
		}
	}
	public Car Dequeue() {
		if(isQueued == false)
			return null;
		Car tempCar = this.nextInLine;
		this.nextInLine = tempCar.nextInLine;
		if(this.nextInLine == null)
			isQueued = false;
		tempCar.isQueued = false;
		//tempCar.inAQueue = false;
		tempCar.nextInLine = null;
		return tempCar;
	}*/
	
	public int distance(Car tempCar) {
		if(this.xy[0] == tempCar.xy[0] && this.dir == tempCar.dir && 
				(this.dir == 'N' || this.dir == 'S'))
			return this.xy[1] - tempCar.xy[1];
		else if(this.xy[1] == tempCar.xy[1] && this.dir == tempCar.dir &&
				(this.dir == 'E' || this.dir == 'W'))
			return this.xy[0] - tempCar.xy[0];
		else if(tempCar.dir == 'N' || tempCar.dir == 'W')
			return Integer.MAX_VALUE;
		else if(tempCar.dir == 'S' || tempCar.dir == 'E')
			return Integer.MIN_VALUE;
		System.out.println("Car Distance Calculation Error");
		return 0;
	}
	
	public void printCar() {
		System.out.print(this.carID);
		System.out.print("\tEN-ID: ");
		System.out.print(this.entrancePoint.pointID);
		System.out.print("\tTP1-ID: ");
		if(this.turningPoint1 == null)
			System.out.print("00000000");
		else
			System.out.print(this.turningPoint1.pointID);
		System.out.print("\tTP2-ID: ");
		if(this.turningPoint2 == null)
			System.out.print("00000000");
		else
			System.out.print(this.turningPoint2.pointID);
		System.out.print("\tEX-ID: ");
		System.out.println(this.exitPoint.pointID);
	}
	
	private static char[] getCarIDFromInt(int i) {
		char[] carID = new char[8];
		char[] carNumber = String.valueOf(i).toCharArray();
		int k=0;
		for(int j=0; j<(8-carNumber.length); j++) {	//zero pending to hundreds and tens position
			carID[j]='0';
			k++;
		}
		for(int j=k; j<8; j++)
			carID[j]=carNumber[j-k];
		return carID;
	}
}
