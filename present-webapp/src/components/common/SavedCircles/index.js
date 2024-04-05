import React, {Component, PropTypes} from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as userActions from '../../../redux/actions/userActions';
import * as groupActions from '../../../redux/actions/groupActions';
import groupUtiles from '../../../core/groupUtiles';
import userUtiles from '../../../core/userUtiles';

class SavedCircles extends Component {

  constructor(props) {
    super(props);

    this.state = {
      savedGroups: props.savedGroups ? props.savedGroups : [],
      nearByGroups: props.nearByGroups ? props.nearByGroups : [],
      nearByChecked: false,
      checkedGroups:[]
    }

    this.onClickNearBy = this.onClickNearBy.bind(this);
  }

  componentWillReceiveProps(nextProps){

    if (this.props.savedGroups !== nextProps.savedGroups) {
      this.setState({savedGroups: nextProps.savedGroups});
        this.getCheckedGroups(nextProps.savedGroups);
    }

    if (this.props.currentGroup !== nextProps.currentGroup) {
      this.setState({currentGroup: nextProps.currentGroup});
    }

    if (this.props.nearByGroups !== nextProps.nearByGroups) {
      this.setState({nearByGroups: nextProps.nearByGroups});
    }
  }

  getCheckedGroups(savedGroups){

    let checkedGroups = this.state.checkedGroups;
    for(let group of savedGroups.groups){
      checkedGroups[group.uuid] = group.unread;
    }

    this.setState({checkedGroups});
  }

  onClickNearBy(){
    this.setState({nearByChecked: !this.state.nearByChecked})
  }

  renderSavedGroups(){
    let showSavedGroups = <span/>,
        getAllSaved = [];

    if(!this.state.nearByChecked){
      getAllSaved = this.state.savedGroups.groups;
    } else {
      if(this.props.nearByGroups.length > 0){
        getAllSaved = groupUtiles.getNearBySavedGroups(this.props.nearByGroups,this.state.savedGroups.groups)
      }
    }

    if(!_.isEmpty(this.state.savedGroups)) {
      showSavedGroups = getAllSaved.map(group => {
        return(
          <div key={group.uuid} className={group.uuid === this.props.currentGroup.uuid && this.props.fromRoute === "circleHome" ? "active-background active" : ""}>
            <button
              key={group.uuid}
              onClick={this.onClickGoToGroup.bind(this,group)}
              className="group-btn">
              {this.state.checkedGroups[group.uuid] ?
                this.props.type === 'circle-home' && group.uuid === this.props.currentGroup.uuid ?
                <span/> :
                <div className="unread-msg"></div>
              : <span/>}
              <p className={this.state.checkedGroups[group.uuid] ? this.props.type === 'circle-home' && group.uuid === this.props.currentGroup.uuid ? "title" : "title active" : "title"}>{group.title}</p>
            </button>
          </div>
        )
      });
    }
    return showSavedGroups;
  }

  onClickGoToGroup(group) {

    let checkedGroups = this.state.checkedGroups;
    checkedGroups[group.uuid] = false;

    this.setState({groupActive:group.id, checkedGroups});

    if(this.props.fromRoute === "circleHome"){
      this.props.loadRequestedGroupFromBackend(group.url);
    }

    this.context.router.push(groupUtiles.getGroupUrl(group));
  }

  render() {

    return(
      <div>
        {!_.isEmpty(this.props.currentUser) && userUtiles.isUserAdmin(this.props.currentUser) ?
          <div className="nearby-checkbox">
            <input type="checkbox"
              onClick={this.onClickNearBy}
              checked={this.state.nearByChecked}
              />
            <label>Nearby Only</label>
          </div>
        : <span/>
        }
        <div className="for-circles white-bg">
          <p className="ur-circle">My Circles</p>
          <div className="cir-names">
            {this.renderSavedGroups()}
          </div>
        </div>
      </div>
    )
  }
}

SavedCircles.contextTypes = {
  router: PropTypes.object
};


function mapStateToProps(state, ownProps) {
    return {
      currentUser: state.user.currentUser,
      savedGroups: state.groups.savedGroups,
      nearByGroups: state.groups.nearByGroups,
      currentGroup: state.group.currentGroup
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({...userActions,...groupActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(SavedCircles);
