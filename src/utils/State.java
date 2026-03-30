package utils;

public class State {
    public enum State_enum {
        S_ACK_NOT_RECEIVING,
        S_ACK_RECEIVE
    }

    State_enum current_state;

    public State() {
        current_state = State_enum.S_ACK_NOT_RECEIVING;
    }

    public State_enum get_state(){
        return current_state;
    }

    public void set_state(State_enum s){
        current_state = s;
    }
}
