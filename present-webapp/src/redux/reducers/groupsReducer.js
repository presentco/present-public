import * as types from '../actions/actionTypes';
import initialState from './initialState';

export default function Group(state = initialState, action) {

    switch (action.type) {

        case types.LOAD_SAVED_GROUP_SUCCESS: {
            return Object.assign({}, state,  {
                savedGroups: action.groups
            });
        }

        case types.LOAD_NEARBY_GROUPS_SUCCESS: {
            return Object.assign({}, state,  {
                nearByGroups: action.groups
            });
        }

        default:
            return state;
    }
}
