import React, { Component,PropTypes} from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as userActions from '../../../redux/actions/userActions';
import ApiHandler from '../../../core/ApiHandler';
import userUtiles from '../../../core/userUtiles';
import Modal from '../../CircleHome/components/Modal';
import _ from 'lodash';

class NamePhoto extends Component {

  constructor(props){
    super(props);
    this.state={
      user: props.user,
      openMenu:false,
      blockedUsers:[]
    }

    this.onClickBlockUser = this.onClickBlockUser.bind(this);
    this.getBlockedUsers();
  }

  adminClickedAction(stateId,userId){

    let request = ApiHandler.attachHeader({userId, stateId});
    this.props.actions.transitionState(request).then(response => {
      this.setState({showSuspendMessgae: true, openMenu:false, actionMessage: stateId.toLowerCase()});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  getBlockedUsers(){
    let request = ApiHandler.attachHeader({})
    this.props.actions.getBlockedUsers(request).then(response => {
      this.setState({blockedUsers: response});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  checkIfUserBlocked(user){
    let blockUsers = this.state.blockedUsers,
        blocked= false;

    for (let blockUser of blockUsers) {
      if(user.id === blockUser.id){
        blocked = true;
        break;
      }
    }
    return blocked;
  }

  onClickBlockUser(){
    let request = ApiHandler.attachHeader({userId: this.props.user.id});
    if(this.checkIfUserBlocked(this.props.user)){
      this.unBlockUser(request);
    } else {
      this.blockUser(request);
    }
  }

  blockUser(request){
    this.props.actions.blockUser(request).then(response => {
      this.setState({isBlocked: true, openMenu:false});
      this.getBlockedUsers();
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  unBlockUser(request){
    this.props.actions.unBlockUser(request).then(response => {
      this.setState({isBlocked: false, openMenu:false});
      this.getBlockedUsers();
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  getUserStates(userId){
    return this.props.validStateTransitions.map((state, index) => {
      return(
        <p className="menu-text" onClick={this.adminClickedAction.bind(this, state.id, userId)}>{_.startCase(state.verb)}
          <span className="hover-decription">{state.description}</span>
        </p>
      )
    })
  }


  render(){
    let user = this.props.user,
        photo = "",
        name = "",
        location = "",
        id,
        showSuspendMessgae = <span/>;

        if(!_.isEmpty(user)){
          photo = user.photo ? user.photo : require('../../../assets/images/phone-veri-photo@2x.png');
          name = typeof user.name === 'string' ?  _.startCase(_.toLower(user.name)) : _.startCase(_.toLower(`${user.name.first} ${user.name.last}`));
          location = user.home ? user.home.name : user.signupLocation ? user.signupLocation : '';
          id = user.id;
        }

        if(this.state.showSuspendMessgae){
          showSuspendMessgae = (
            <Modal show={this.state.showSuspendMessgae}
                   noPadding={true}
                   closeModal={() => this.setState({showSuspendMessgae: false})}
                   width={true}
                   clickInput={this.state.clickInput}>
                    <div className="text-center msg-sus-container">
                      <button className="x-btn-sus" onClick={() => this.setState({showSuspendMessgae:false})}>X</button>
                      <p>{name} has been {this.state.actionMessage}.</p>
                    </div>
            </Modal>
          )
        }

    return(
      <div className="user-photo-contrainer text-center">
        {showSuspendMessgae}
        {!_.isEmpty(this.props.currentUser) && this.props.currentUser.id !== id ?
          <div className="burger-icon-container" onClick={() => this.setState({openMenu: !this.state.openMenu})}>
            <img src={require('../../../assets/images/hamburger.svg')}
              alt="menu"
              className="burgar-menu" />
          </div> :
          <span/>
        }
        {this.state.openMenu ?
        <div className="menu-user-container">
          <p className="menu-text" onClick={this.onClickBlockUser}>{this.state.isBlocked ? "Unblock" : this.checkIfUserBlocked(user) ? "Unblock" : "Block"}</p>
          {userUtiles.isUserAdmin(this.props.currentUser) && !_.isEmpty(this.props.validStateTransitions)?
            this.getUserStates(id)
            : <span/>

          }
        </div> :
      <span/>}
        <img src={photo} alt={name} className="user-profile-photo"/>
        <p className="user-profile-name">{name}</p>
        {location !== '' ?
        <div>
        <img src={require('../../../assets/images/page-1.svg')} alt={location} className="pin-profile"/>
        <span className="pin-text">{location}</span>
        </div> : <span/>}
      </div>
    )
  }
}

NamePhoto.contextTypes = {
  router: PropTypes.object
};


function mapStateToProps(state, ownProps) {
  return {
    currentUser: state.user.currentUser
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(userActions, dispatch)
  };
}

export default connect(mapStateToProps,mapDispatchToProps)(NamePhoto);
