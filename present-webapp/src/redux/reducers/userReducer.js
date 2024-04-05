
import * as types from '../actions/actionTypes';
import initialState from './initialState';

export default function User(state = initialState.currentUser, action) {

    switch (action.type) {

        case types.USER_LOGIN_SUCCESS: {

            return Object.assign({}, state,  {
                currentUser: action.user
            });
        }

        case types.USER_LOGOUT_SUCCESS: {
            return Object.assign({}, initialState, {});
        }


        default:
            return state;
    }
}
