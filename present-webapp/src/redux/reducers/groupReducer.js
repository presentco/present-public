import * as types from '../actions/actionTypes';
import initialState from './initialState';

export default function Group(state = initialState, action) {

    switch (action.type) {

        case types.LOAD_GROUP_SUCCESS: {
            return Object.assign({}, state,  {
                currentGroup: action.group,
                groupId: action.group.uuid
            });
        }

        case types.LOAD_SOCKET_SUCCESS:{
          return Object.assign({}, state,  {
              socketUrl: `wss://${action.host.host}:${action.host.port}/comments`
          });
        }

        case types.LOAD_MEMBER_REQUESTS:{
          return Object.assign({}, state, {
            memberRequests: action.requests
          })
        }

        default:
            return state;
    }
}
