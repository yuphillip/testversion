package sample;

import java.util.List;
import java.util.ArrayList;
import javafx.scene.input.MouseEvent;
import java.util.function.Predicate;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;


@SuppressWarnings("restriction")
public class Controller {

    //the board which this controller is bound to
    private final CheckerBoard board;
    //a list of all valid moves that can be made
    private List<PointCB> absoluteValid = new ArrayList<PointCB>();
    //is the currently selected checker a valid piece to be moved?
    private boolean validSelection;
    //a global refernce to the last checker marked to be a valid movement as well it's previous coordinates
    private Checker cleared;
    private int prevI, prevJ;
    private List<PointCB> relativeValid;

    Controller(CheckerBoard tmp) {
        board = tmp;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setValidSelection() {
        //clear old list
        absoluteValid.clear();
        //determine which pieces to scan
        List<Checker> team;
        Predicate<Node> filterCondition = (x) -> {
            if(x.getClass() == Checker.class) {
                return ((Checker)x).team.equals(board.getTurn());
            }
            else return false;
        };
        team = (List)(board.getChildren().filtered(filterCondition));
        //scan third row viability of each piece
        for(Checker checker : team) {
            absoluteValid.addAll(scanR2(checker));
        }
        //if no jumps are avaible then the list of valid movements it the list of single jump spaces availbe
        if(absoluteValid.isEmpty()) {
            for(Checker checker : team) {
                absoluteValid.addAll(scanR1(checker));
            }
        }
    }

    //scans ring 1 of adjacent grid cells for valid movements
    public List<PointCB>  scanR1(Checker checker) {
        //get coordinates of checker in grid
        int i = board.getColumnIndex(checker);
        int j = board.getRowIndex(checker);
        List<PointCB> ret = new ArrayList<PointCB>();
        //scanner loop ... y axis
        for(int dY=-1; dY<2; dY+=2) {
            //if checker is not a checker.getKing() only scan subjectively forward
            if(!checker.getKing()) {
                if(checker.team.equals("red") && dY==-1) continue;
                else if(checker.team.equals("black") && dY==1) continue;
            }
            if(j+dY<0) continue;
            else if(j+dY>7) break;
            //x-axis loop
            for(int dX=-1; dX<2; dX+=2) {
                //add empty spaces to list
                if(board.getCell(Checker.class, i+dX, j+dY).isEmpty()) ret.add(new PointCB(checker, i+dX, j+dY));
            }
        }
        return ret;
    }
    //scans ring 3 of adjacent grid cells for valid movements
    public List<PointCB>  scanR2(Checker checker) {
        //get coordinates of checker in grid
        int i = board.getColumnIndex(checker).intValue();
        int j = board.getRowIndex(checker).intValue();
        List<PointCB> ret = new ArrayList<PointCB>();
        String searchFor = checker.team.equals("red") ? "black" : "red";
        //scanner loop ... y axis
        for(int dY=-2; dY<3; dY+=4) {
            //if checker is not a checker.getKing() only scan subjectively forward
            if(!checker.getKing()) {
                if(checker.team.equals("red") && dY==-2) continue;
                else if(checker.team.equals("black") && dY==2) continue;
            }
            if(j+dY<0) continue;
            else if(j+dY>7) break;
            //x-axis loop
            for(int dX=-2; dX<3; dX+=4) {

                if(i+dX<0) continue;
                else if(i+dX>7) break;
                //add empty spaces to list
                if(!board.getCell(Checker.class, i+dX/2, j+dY/2).isEmpty()) {
                    if(board.getCell(Checker.class, i+dX/2, j+dY/2).get(0).team.equals(searchFor)) {
                        if(board.getCell(Checker.class, i+dX, j+dY).isEmpty()) ret.add(new PointCB(checker, i+dX, j+dY));
                    }
                }
            }
        }
        return ret;
    }

    public <T extends MouseEvent>void recieveControl(T e) {
        //Save event type as String for logical testing
        String eventType = e.getEventType().getName();
        //forward the event to the correct method for final processing
        switch(eventType) {

            case "DRAG_DETECTED" : controlDragDetected(e);
                break;
            case "MOUSE_DRAGGED" : controlMouseDragged(e);
                break;
            case "MOUSE_RELEASED" : controlMouseReleased(e);
                break;
        }
    }
    public List<PointCB> getRelativeValid(Checker checker) {
        //list for holding return value
        List<PointCB> ret = new ArrayList<PointCB>();
        //add all available jumps to list
        ret.addAll(scanR2(checker));
        //if list is empty add all available adjacent spaces
        if(ret.isEmpty()) ret.addAll(scanR1(checker));
        return ret;
    }
    /********************************************************************************************
     * ******************************************************************************************
     *              DRAG CONTROLS
     * ******************************************************************************************
     * ******************************************************************************************/
    //PROBLEM
    public void controlDragDetected(MouseEvent e) {
        //get relative valid movements for comparison
        Checker checker = (Checker)e.getSource();
        int i = board.getColumnIndex(checker);
        int j = board.getRowIndex(checker);
        relativeValid = getRelativeValid(checker);
        List<PointCB> tmp = new ArrayList<PointCB>();
        //compare relative vlaid movements to absolute get the true list of relatively valid movements
        for(PointCB local : relativeValid) {
            for(PointCB remote : absoluteValid) {
                if(remote.equals(local)) tmp.add(local);
            }
        }
        relativeValid = tmp;
        //if there are valid moves relative to the selected piece, enable it to move freely
        if(!relativeValid.isEmpty()) {
            for(PointCB p : relativeValid) {
                board.getCell(Rectangle.class, p).forEach((x) -> x.setFill(Color.CHARTREUSE));
            }
            checker.setManaged(false);
            //mark this as being the last cleared piece
            cleared = checker;
            prevI = i;
            prevJ = j;
        }
        else {
            board.getCell(Rectangle.class, i, j).forEach((x) -> x.setFill(Color.CRIMSON));
        }
    }
    //if the piece has been cleared as valid allow it to be moved
    public void controlMouseDragged(MouseEvent e) {
        Checker checker = (Checker)e.getSource();
        if(checker == cleared) {
            checker.setCenterX(e.getX());
            checker.setCenterY(e.getY());
        }
        else e.consume();
    }
    //when the mouse is released, place it according to wheather it was relased into a valid cell
    public void controlMouseReleased(MouseEvent e) {
        Checker checker = (Checker)e.getSource();
        if(checker == cleared) {
            boolean set = false;
            int i = (int)(e.getSceneX()/board.cellWidth());
            int j = (int)(e.getSceneY()/board.cellHeight());
            //check if the drop location is valid
            PointCB currentCoord = new PointCB(checker, i, j);
            for(PointCB p : relativeValid) {
                if(currentCoord.equals(p)) {
                    board.getChildren().remove(checker);
                    board.add(checker, i, j);
                    set = true;
                }
            }
            //reset the board to it's default coloring
            if(!set) {
                board.getChildren().remove(checker);
                board.add(checker, prevI, prevJ);
            }
            checker.setManaged(true);
            if(Math.abs(board.getColumnIndex(checker)-prevI)==2 && Math.abs(board.getRowIndex(checker)-prevJ)==2) {
                handleJump(checker);
            }
            else if( !(board.getColumnIndex(checker)==prevI || board.getRowIndex(checker)==prevJ)) changeTurn();
            checkCrown();
        }
        resetTiles();
    }
    /*******************************************************************************************
     *                END CONTROLS
     *******************************************************************************************/
    //reset the tiles to their default colors
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void resetTiles() {
        List<Rectangle> tmp = (List)board.getChildren().filtered((x) ->{
            //only maroon pieces can be valid movements therefore any piece that is not moccasin should be maroon
            if(x.getClass() == Rectangle.class) {
                if(!((Rectangle)x).getFill().equals(Color.MOCCASIN)) return true;
                else return false;
            }
            else return false;
        });
        for(Rectangle r : tmp) r.setFill(Color.MAROON);
    }
    //if a jump is detected, determine if further jumps need be executed
    public void handleJump(Checker checker) {
        //deal with a jump event
        int dX = board.getColumnIndex(checker)-prevI;
        int dY = board.getRowIndex(checker)-prevJ;
        //remove the piece that was jumped over
        board.getChildren().removeAll(board.getCell(Checker.class, (prevI+dX/2), (prevJ+dY/2)));
        //clear previous valid selection
        //limit new valid selection to this piece and only jumps
        absoluteValid.clear();
        absoluteValid.addAll(scanR2(checker));
        if(absoluteValid.isEmpty()) changeTurn();
    }
    //change the turn, progress the game
    public void changeTurn() {
        String setTo = board.getTurn().equals("red") ? "black" : "red";
        board.getTurnProperty().setValue(setTo);
        setValidSelection();
    }
    //crown the piece if need be
    public void checkCrown() {
        int kingColumn = cleared.team.equals("red") ? 7 : 0;
        int j = board.getRowIndex(cleared);
        if(j == kingColumn) {
            cleared.setKing(true);
            System.out.println("PIECE KINGED SUCCESFULLY");
        }
    }
}