

/*

AUTHOR:      John Lu

DESCRIPTION: This file contains your agent class, which you will
             implement.

NOTES:       - If you are having trouble understanding how the shell
               works, look at the other parts of the code, as well as
               the documentation.

             - You are only allowed to make changes to this portion of
               the code. Any changes to other portions of the code will
               be lost when the tournament runs your code.
*/

package src;
import java.util.ArrayList;

import src.Action.ACTION;


public class MyAI extends AI {
	// ########################## INSTRUCTIONS ##########################
	// 1) The Minesweeper Shell will pass in the board size, number of mines
	// 	  and first move coordinates to your agent. Create any instance variables
	//    necessary to store these variables.
	//
	// 2) You MUST implement the getAction() method which has a single parameter,
	// 	  number. If your most recent move is an Action.UNCOVER action, this value will
	//	  be the number of the tile just uncovered. If your most recent move is
	//    not Action.UNCOVER, then the value will be -1.
	// 
	// 3) Feel free to implement any helper functions.
	//
	// ###################### END OF INSTURCTIONS #######################
	
	// This line is to remove compiler warnings related to using Java generics
	// if you decide to do so in your implementation.
	@SuppressWarnings("unchecked")
	private int rowDimension;
	private int colDimension;
	private int totalMines;
	private int numMines;
	private int startX;
	private int startY;
	private int currentX;
	private int currentY;
	private int uncoverCount = 0;
	private ArrayList<TwoTuple> uncoverQueue;
	private ArrayList<TwoTuple> flagQueue;
	
	private Tile[][] board;
	
	// ------------------- Inner Class: Tile  --------------------
	/* 	Description:
	 * 	A Tile object represent a "square" on the game board.
	 */
	private class Tile {
		public boolean queued = false;
		public boolean covered = true;
		public boolean flagged = false;
		public int number = 9;
		public int numBombs = number;
	}
	
	
	// ---------------- Inner Class: TwoTuple  -------------------
	/* 	Description:
	 * 	A TwoTuple is a length-2 tuple of integers used to represent
	 * 	an (x,y) coordinate or, alternatively, a (row, col) coordinate
	 * 	of a Tile in the board.
	 * 	
	 * 	Additional Info:
	 * 	The internal representation of a board is a 2-d array, 0-indexed 
	 * 	array. However, users, specify locations on the board using 1-indexed
	 * 	(x,y) Cartesian coordinates. 
	 */
	private class TwoTuple {
		public int row;
		public int col;
		public TwoTuple(int row, int col) { this.row = row; this.col = col; }
		
		public String toString() {
			return "(" + col + "," + row + ")";
		}
	}
	

	public MyAI(int rowDimension, int colDimension, int totalMines, int startX, int startY) {
		this.rowDimension = rowDimension;
		this.colDimension = colDimension;
		this.totalMines = totalMines;
		this.numMines = totalMines;
		this.startX = startX;
		this.startY = startY;
		this.currentX = startX;
		this.currentY = startY;
		
		this.board = new Tile[rowDimension][colDimension];
		this.uncoverQueue = new ArrayList<TwoTuple>();
		this.flagQueue = new ArrayList<TwoTuple>();
		initBoard();
		System.out.println("Start X: " + startX);
		System.out.println("Start Y: " + startY);

	}
	
	
	public void initBoard() {
		for(int i = 0; i < rowDimension; i++) {
			for(int j = 0; j < colDimension; j++) {
				board[i][j] = new Tile();
			}
		}
	}
	
	
	
	// ################## Implement getAction(), (required) #####################
	public Action getAction(int number) {
		int row = translateToRow(currentY);
		int col = translateToCol(currentX);
		
		if(number >= 0) { // This means if we uncovered something
			board[row][col].covered = false;
			board[row][col].queued = true;
			board[row][col].number = number;
			board[row][col].numBombs = number - numFlagSurround(row, col);
		}
		
		if(number == 0) {
			queueUncoverSurroundings(row, col);
		}
		else if(number == -1) {
			board[row][col].flagged = true;
			reduceSurroundingNums(row, col);
		}
		
		//printBoard();
		
		
		if(uncoverQueue.isEmpty()) { queueSafeSquares(); }
		if(flagQueue.isEmpty()){ queueKnownMines(); }
		if(uncoverQueue.isEmpty() && flagQueue.isEmpty()) {queueOneTwoPattern();}
		
		
		if(!uncoverQueue.isEmpty()) {
			currentX = translateToX(uncoverQueue.get(0).col);
			currentY = translateToY(uncoverQueue.get(0).row);
			uncoverQueue.remove(0);
			uncoverCount++;
			return new Action(ACTION.UNCOVER, currentX, currentY);
		}
		
		if(!flagQueue.isEmpty()) {
			currentX = translateToX(flagQueue.get(0).col);
			currentY = translateToY(flagQueue.get(0).row);
			flagQueue.remove(0);
			numMines--;
			return new Action(ACTION.FLAG, currentX, currentY);
		}
		

		TwoTuple random = randomCoveredSpot();
		currentX = translateToX(random.col);
		currentY = translateToY(random.row);
		return new Action(ACTION.UNCOVER, currentX, currentY);
		
		//redundant move to signify passing a turn to make a move.
		//return new Action(ACTION.UNCOVER, startX, startY);
		
		
		//Should never reach here.
		//return new Action(ACTION.LEAVE);
	}
	
	

	
	
	
	
	public boolean isLeftUncovered(int row, int col) {
		for(int i = row - 1; i <= row + 1; i++) {
			if(isInBounds(i, col - 1)) {
				if(board[i][col - 1].covered) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean isRightUncovered(int row, int col) {
		for(int i = row - 1; i <= row + 1; i++) {
			if(isInBounds(i, col + 1)) {
				if(board[i][col + 1].covered) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean isUpUncovered(int row, int col) {
		for(int j = col - 1; j <= col + 1; j++) {
			if(isInBounds(row - 1, j)) {
				if(board[row - 1][j].covered) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	public boolean isDownUncovered(int row, int col) {
		for(int j = col - 1; j <= col + 1; j++) {
			if(isInBounds(row + 1, j)) {
				if(board[row + 1][j].covered) {
					return false;
				}
			}
		}
		return true;
	}
	
	
	
	//looks through the board for tiles that are safe to queue to uncover
	public void queueSafeSquares() {
		for(int i = 0; i < rowDimension; i++) {
			for(int j = 0; j < colDimension; j++) {
				if(!board[i][j].covered && board[i][j].number != 0) {
					if(board[i][j].numBombs == 0) {
						queueUncoverSurroundings(i, j);
					}
				}
			}
		}
	}
	
	
	//queues up the surroundings of (row,col) to be uncovered later
	public void queueUncoverSurroundings(int row, int col) {

		for(int i = row - 1; i <= row + 1; i++) {
			for(int j = col - 1; j <= col + 1; j++) {

				if((i != row || j != col) && isInBounds(i, j) 
						&& board[i][j].covered && !board[i][j].flagged && !board[i][j].queued) {
					board[i][j].queued = true;
					uncoverQueue.add(new TwoTuple(i, j));
				}
			}
		}
	}
	

	
	//looks through the board for tiles that are guaranteed mines and queues them.
	public void queueKnownMines() {
		for(int i = 0; i < rowDimension; i++) {
			for(int j = 0; j < colDimension; j++) {
				if(!board[i][j].covered && board[i][j].number != 0) {
					if(numCoveredSurround(i, j) == board[i][j].numBombs) {

						queueFlagSurroundings(i, j);
					}
				}
			}
		}
	}
	
	//adds all covered tiles surrounding (row, col) to flag queue.
	public void queueFlagSurroundings(int row, int col) {
		for(int i = row - 1; i <= row + 1; i++) {
			for(int j = col - 1; j <= col + 1; j++) {
				if((i != row || j != col) && isInBounds(i, j) 
						&& board[i][j].covered && !board[i][j].flagged && !board[i][j].queued) {
					board[i][j].queued = true;
					flagQueue.add(new TwoTuple(i, j));
				}
			}
		}
	}
	
	public void reduceSurroundingNums(int row, int col) {
		for(int i = row - 1; i <= row + 1; i++) {
			for(int j = col - 1; j <= col + 1; j++) {
				if((i != row || j != col) && isInBounds(i, j) 
						&& !board[i][j].covered && !board[i][j].flagged) {
					board[i][j].numBombs = board[i][j].numBombs - 1;
				}
			}
		}
	}
	
	//return the number of flagged tiles around the (row, col) tile.
	public int numFlagSurround(int row, int col) {
		int count = 0;
		for(int i = row - 1; i <= row + 1; i++) {
			for(int j = col - 1; j <= col + 1; j++) {
				if((i != row || j != col) && isInBounds(i, j) && board[i][j].flagged) {
					count++;
				}
			}
		}
		return count;
	}
	
	
	
	//returns number of covered tiles around the (row, col) tile.
	public int numCoveredSurround(int row, int col) {
		int count = 0;
		for(int i = row - 1; i <= row + 1; i++) {
			for(int j = col - 1; j <= col + 1; j++) {
				if((i != row || j != col) && isInBounds(i, j) 
						&& board[i][j].covered && !board[i][j].flagged) {
					count++;
				}
			}
		}
		return count;
	}
	
	public TwoTuple randomCoveredSpot() {
		ArrayList<TwoTuple> random = new ArrayList<TwoTuple>();
		for(int row = 0; row < rowDimension; row++) {
			for(int col = 0; col < colDimension; col++) {
				if(board[row][col].covered && !board[row][col].flagged) {
					random.add(new TwoTuple(row, col));
				}
			}
		}
		int index = (int)(Math.random() * random.size());
		return random.get(index);
	}
	
	
	public TwoTuple randomSpot() {
		int row = (int) (Math.random() * rowDimension);
		int col = (int) (Math.random() * colDimension);
		TwoTuple temp = new TwoTuple(row, col);
		return temp;
	}
	
	public boolean isInBounds(int row, int col) {
		return row >= 0 && row < rowDimension && col >= 0 && col < colDimension;
	}
	
	
	public int translateToCol(int x) {
		return x - 1;
	}
	
	
	public int translateToRow(int y) {
		return this.rowDimension - y;
		
	}
	
	
	public int translateToX(int col) {
		return col + 1;
	}
	
	
	public int translateToY(int row) {
		return this.rowDimension - row;
	}
	
	
	private TwoTuple translateCoordinate(int x, int y) {
		/*	Inputs:
		 * 		x - x coordinate of board
		 * 		y - y coordinate of board 
		 * 
		 * 	Outputs:
		 * 		TwoTuple, t, where:
		 * 			t.x is the corresponding row index in the board
		 * 			t.y is the corresponding col index in the board
		 * 
		 * 	Description:
		 * 	Translates the given (x,y) coordinate to a (row, col) tuple used
		 * 	for indexing into the board instance variable. (See Note below).
		 * 
		 * 	Notes:
		 * 	The internal representation of a board is a 2-d array, 0-indexed 
		 * 	array. However, users, specify locations on the board using 1-indexed
		 * 	(x,y) Cartesian coordinates. 
		 * 	Hence, to access the proper indicies into the board array, a translation 
		 * 	must be performed first.
		 */
		int row = this.rowDimension-y;
		int col = x-1;
		return new TwoTuple(row, col);
	}
	
	public void queueOneTwoPattern() {
		for(int i = 0; i < rowDimension; i++) {
			for(int j = 0; j < colDimension; j++) {
				if(board[i][j].numBombs == 2) {
					if(isInBounds(i, j - 1) && board[i][j - 1].numBombs == 1 && isInBounds(i, j + 1) && (!board[i][j + 1].covered || board[i][j + 1].flagged)) {
						if(isDownUncovered(i, j) && isInBounds(i - 1, j + 1) && !board[i - 1][j + 1].queued) {
							board[i - 1][j + 1].queued = true;
							flagQueue.add(new TwoTuple(i - 1, j + 1));
							if(isInBounds(i - 1, j - 2) && board[i - 1][j - 2].covered && !board[i - 1][j - 2].queued) {
								board[i - 1][j - 2].queued = true;
								uncoverQueue.add(new TwoTuple(i - 1, j - 2));
							}
						}
						if(isUpUncovered(i, j) && isInBounds(i + 1, j + 1) && !board[i + 1][j + 1].queued) {
							board[i + 1][j + 1].queued = true;
							flagQueue.add(new TwoTuple(i + 1, j + 1));
							if(isInBounds(i + 1, j - 2) && board[i + 1][j - 2].covered && !board[i + 1][j - 2].queued) {
								board[i + 1][j - 2].queued = true;
								uncoverQueue.add(new TwoTuple(i + 1, j - 2));
							}
						}
					}
					
					if(isInBounds(i, j + 1) && board[i][j + 1].numBombs == 1 && isInBounds(i, j - 1) && (!board[i][j - 1].covered || board[i][j - 1].flagged)) {
						if(isDownUncovered(i, j) && isInBounds(i - 1, j - 1) && !board[i - 1][j - 1].queued) {
							board[i - 1][j - 1].queued = true;
							flagQueue.add(new TwoTuple(i - 1, j - 1));
							if(isInBounds(i - 1, j + 2) && board[i - 1][j + 2].covered && !board[i - 1][j + 2].queued) {
								board[i - 1][j + 2].queued = true;
								uncoverQueue.add(new TwoTuple(i - 1, j + 2));
							}
						}
						if(isUpUncovered(i, j) && isInBounds(i + 1, j - 1) && !board[i + 1][j - 1].queued) {
							board[i + 1][j - 1].queued = true;
							flagQueue.add(new TwoTuple(i + 1, j - 1));
							if(isInBounds(i + 1, j + 2) && board[i + 1][j + 2].covered && !board[i + 1][j + 2].queued) {
								board[i + 1][j + 2].queued = true;
								uncoverQueue.add(new TwoTuple(i + 1, j + 2));
							}
						}
					}
					
					if(isInBounds(i - 1, j) && board[i - 1][j].numBombs == 1 && isInBounds(i + 1, j) && (!board[i + 1][j].covered || board[i + 1][j].flagged)) {
						if(isLeftUncovered(i, j) && isInBounds(i + 1, j + 1) && !board[i + 1][j + 1].queued) {
							board[i + 1][j + 1].queued = true;
							flagQueue.add(new TwoTuple(i + 1, j + 1));
							if(isInBounds(i - 2, j + 1) && board[i - 2][j + 1].covered && !board[i - 2][j + 1].queued) {
								board[i - 2][j + 1].queued = true;
								uncoverQueue.add(new TwoTuple(i - 2, j + 1));
							}
						}
						if(isRightUncovered(i, j) && isInBounds(i + 1, j - 1) && !board[i + 1][j - 1].queued) {
							board[i + 1][j - 1].queued = true;
							flagQueue.add(new TwoTuple(i + 1, j - 1));
							if(isInBounds(i - 2, j - 1) && board[i - 2][j - 1].covered && !board[i - 2][j - 1].queued) {
								board[i - 2][j - 1].queued = true;
								uncoverQueue.add(new TwoTuple(i - 2, j - 1));
							}
						}
					}
					
					if(isInBounds(i + 1, j) && board[i + 1][j].numBombs == 1 && isInBounds(i - 1, j) && (!board[i - 1][j].covered || board[i - 1][j].flagged)) {
						if(isLeftUncovered(i, j) && isInBounds(i - 1, j + 1) && !board[i - 1][j + 1].queued) {
							board[i - 1][j + 1].queued = true;
							flagQueue.add(new TwoTuple(i - 1, j + 1));
							if(isInBounds(i + 2, j + 1) && board[i + 2][j + 1].covered && !board[i + 2][j + 1].queued) {
								board[i + 2][j + 1].queued = true;
								uncoverQueue.add(new TwoTuple(i + 2, j + 1));
							}
						}
						if(isRightUncovered(i, j) && isInBounds(i - 1, j - 1) && !board[i - 1][j - 1].queued) {
							board[i - 1][j - 1].queued = true;
							flagQueue.add(new TwoTuple(i - 1, j - 1));
							if(isInBounds(i + 2, j - 1) && board[i + 2][j - 1].covered && !board[i + 2][j - 1].queued) {
								board[i + 2][j - 1].queued = true;
								uncoverQueue.add(new TwoTuple(i + 2, j - 1));
							}
						}
					}
				}
			}
		}
	}
	
	public void printBoard() {
		for(int i = 0; i < rowDimension; i++) {
			for(int j = 0; j < colDimension; j++) {
				if(board[i][j].flagged) {
					System.out.print("X ");
				}
				else if(board[i][j].number == 9) {
					System.out.print("? ");
				}else {
					System.out.print(board[i][j].number + " ");
				}
				
			}
			System.out.println();
		}
	}

	// ################### Helper Functions Go Here (optional) ##################
	// ...
}
