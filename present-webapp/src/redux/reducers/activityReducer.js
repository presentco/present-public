import * as types from '../actions/actionTypes';
import initialState from './initialState';

export default function Group(state = initialState, action) {

    switch (action.type) {

        case types.LOAD_NOTIFICATION_SUCCESS: {
            return Object.assign({}, state,  {
                notifications: action.notifications
            });
        }

        case types.LOAD_INBOX_SUCCESS: {
            return Object.assign({}, state,  {
                inbox: action.inbox
            });
        }

        default:
            return state;
    }
}
