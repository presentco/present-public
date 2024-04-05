import * as types from '../actions/actionTypes';
import initialState from './initialState';

export default function Group(state = initialState.comments, action) {

    switch (action.type) {

        case types.LOAD_GROUP_COMMENTS_SUCCESS: {

            return Object.assign({}, state,  {
                comments: action.comments
            });
        }


        default:
            return state;
    }
}
