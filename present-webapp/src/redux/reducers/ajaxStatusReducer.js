import * as types from '../actions/actionTypes';
import initialState from './initialState';

function actionTypeEndsInSuccess(type) {
    return type.substring(type.length - 8) === '_SUCCESS';
}

export default function User(state = initialState.numAjaxCallsInProgress, action) {

    if (action.type === types.BEGIN_AJAX_CALL) {
        return state + 1;
    } else if (action.type === types.AJAX_CALL_ERROR || actionTypeEndsInSuccess(action.type)) {
        //If the action type is an AJAX ERROR or ends in _SUCCESS it means that the API call has ended.
        //so reduce the number of ajax calls currently in progress
        return state - 1;
    }

    return state;
}
