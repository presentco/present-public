import React, { Component,PropTypes } from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../../redux/actions/userActions';
import * as activityActions from '../../../redux/actions/activityActions';
import * as groupsActions from '../../../redux/actions/groupsActions';
import * as groupActions from '../../../redux/actions/groupActions';
import * as commentActions from '../../../redux/actions/commentActions';
import ApiHandler from '../../../core/ApiHandler';
import groupUtiles from '../../../core/groupUtiles';
import userUtiles from '../../../core/userUtiles';
import './style.css';
import _ from 'lodash';
import Notifications from './Components/Notifications';
import InviteFriends from '../../../components/CircleHome/components/GeneralComponent/Components/inviteFriends';
import SearchBox from './Components/SearchBox';
import EditCircle from '../../../components/CircleHome/components/chatComponent/Components/EditCircle';
import Modal from '../../CircleHome/components/Modal';
import Spinner from 'halogen/BounceLoader';
import Profile from './Components/Profile';
import UserProfile from '../../../components/UserProfile';

class Navbar extends Component {
  constructor(props){
    super(props);

    this.state={
      currentUser: props.currentUser,
      inbox:[],
      profileOption:false,
      showNotification:false,
      showInbox:false,
      showSearch:false,
      getUserNotifications:{},
      notifications:[],
      showMore:false,
      showGroupJoiner:false,
      createCircle: false,
      clickInput:false
    }

    this.onSendQuery = this.onSendQuery.bind(this);
    this.showGroupJoiner= this.showGroupJoiner.bind(this);
    this.onClickSaveCircle = this.onClickSaveCircle.bind(this);
    this.saveEditedCircleWithPhoto = this.saveEditedCircleWithPhoto.bind(this);
    this.onClickProfile = this.onClickProfile.bind(this);
    this.onClickSignOut = this.onClickSignOut.bind(this);
    this.handleClickOutside = this.handleClickOutside.bind(this);
    this.setWrapperRef = this.setWrapperRef.bind(this);
    this.onClickCloseShare = this.onClickCloseShare.bind(this);

    if(localStorage.getItem('currentUser')){
      this.loadUserNotififications();
    }
  }

  componentDidMount(){
    document.addEventListener('mousedown', this.handleClickOutside);

    if(localStorage.getItem('currentUser')){
      this.loadUserProfileFromBackend();
      this.loadUserSavedGroups();
      this.interval = setInterval(() => this.loadUserSavedGroups('markRead'), 10000);
    }

    if(window.location.href.indexOf("/app/createCircle") !== -1){
      this.setState({createCircle:true});
    }
  }

   componentWillUnmount() {
       document.removeEventListener('mousedown', this.handleClickOutside);
       clearTimeout();
       clearInterval(this.interval);
   }

   setWrapperRef(node) {
       this.profileWrapperRef = node;
   }

   handleClickOutside(event) {
       if (this.profileWrapperRef && !this.profileWrapperRef.contains(event.target)) {
           this.setState({showProfileOptions:false});
       }
   }

  componentWillReceiveProps(nextProps){
    if (this.props.currentUser !== nextProps.currentUser) {

      if(!_.isEmpty(nextProps.currentUser)){
        let user = {};
          const userName = `${nextProps.currentUser.name.first} ${nextProps.currentUser.name.last}`,
                bio =`${nextProps.currentUser.bio}`
          user={userName, bio};
          this.setState({currentUser: nextProps.currentUser, user});
      }
    }

    if (this.props.savedGroups !== nextProps.savedGroups) {
      this.setState({savedGroups: nextProps.savedGroups});
    }
  }

  onClickCloseShare(){
    this.setState({memberModalVisible:false});
    this.context.router.push(groupUtiles.getGroupUrl(this.state.circle));
  }

  onClickSignOut(){
    if(window.location.href.indexOf("app") === -1){
      this.context.router.replace('/app');
    }
    window.location.reload();
    this.props.actions.logout();
  }

  loadUserProfileFromBackend(){
    let request = ApiHandler.attachHeader({});
    this.props.actions.loadCurrentUser(request).then(currentUser => {
      this.setState({currentUser});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  onClickProfile(){

    if(window.location.href.indexOf('/u/') > -1){
      this.props.loadRequestedUserFromUrl();
    } else {
        this.context.router.push(userUtiles.getUserLink(this.props.currentUser));
    }
    this.setState({showProfileOptions:false});

  }

  loadUserSavedGroups(markRead){
    let home = !_.isEmpty(this.state.currentUser) ? this.state.currentUser.home.location : {},
        requestTest = ApiHandler.attachHeader({location: home});

    this.props.actions.getSavedGroups(requestTest, markRead).then( response => {
      this.setState({
        savedGroups: response.groups
      });

    }).catch(error => {

      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  loadGroupComments(groupId){
    let request = ApiHandler.attachHeader({groupId});
    this.props.actions.loadRequestedGroupChats(request).then( comments => {
      if(comments){
         this.setState({comments});
      }
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  loadUserNotififications(){
    let request = ApiHandler.attachHeader({});
    this.props.actions.getUserNotifications(request).then(notifications => {
        this.setState({notifications});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  showGroupJoiner(joiner){
    this.setState({showNotifications:false, showGroupJoiner:true, groupJoiner: joiner});
  }

  onSendQuery(result){
    this.props.onSendQuery(result);
  }

  getGroupJoinerName(joiner){
    let name;
    name = joiner.summary.split(' ');
    name = `${name[0]} ${name[1]}`;
    return name;
  }

  onClickSaveCircle(updatedCircle){
    this.setState({circleLoading: true});
    let newCircle = updatedCircle;
    newCircle.center = updatedCircle.location;
    newCircle.createdFrom = updatedCircle.location;
    newCircle.radius = 0;

    let request = ApiHandler.attachHeader(newCircle);

    this.props.actions.updateGroupInfo(request).then(response => {
      this.loadUserSavedGroups();
      this.loadRequestedGroupFromBackend(response.uuid);
      this.loadGroupComments(response.uuid);
      this.setState({circleLoading:false, createCircle:false, circle:response, showAddMembers:true});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
          console.log(errorMessage);
    });
  }

  saveEditedCircleWithPhoto(updatedCircle, cover){
    this.setState({circleLoading: true})
    let newCircle = updatedCircle;
    newCircle.center = updatedCircle.location;
    newCircle.createdFrom = updatedCircle.location;
    newCircle.radius = 0;

    let requestContent = ApiHandler.attachHeader({
        uuid: ApiHandler.guid(),
        type: 'JPEG',
        content:cover.split(',')[1]
      });

    this.props.actions.uploadNewfile(requestContent).then(response => {
      newCircle.cover = {
        uuid: response.uuid,
        type: response.contentType
      };

        let request = ApiHandler.attachHeader(newCircle);
        this.props.actions.updateGroupInfo(request).then(response => {
          this.loadUserSavedGroups();
          this.loadGroupComments(response.uuid);
          this.loadRequestedGroupFromBackend(response.uuid);
          this.setState({circleLoading:false, createCircle:false,showAddMembers:true, circle:response})
          this.context.router.push(`/g/${this.getGroupUrl(response)}`);
        }).catch(error => {
          const errorMessage = ApiHandler.getErrorMessage(error);
              console.log(errorMessage);
        });
    }).catch(error => {
         const errorMessage = ApiHandler.getErrorMessage(error);
         console.log(errorMessage);
      });
  }

  loadRequestedGroupFromBackend(groupId){
    let request = ApiHandler.attachHeader({groupId});
     this.props.actions.loadRequestedGroup(request).then(response => {
     }).catch(error => {
       const errorMessage = ApiHandler.getErrorMessage(error);
           console.log(errorMessage);
     });
  }

  showProfileOptions(){
    return(
      <div className="profile-option-modals" ref={this.setWrapperRef}>
        <p className="profile-options-text" onClick={this.onClickProfile}>View Profile</p>
        <p className="profile-options-text" onClick={() => this.setState({profileOption:true})}>Edit Profile</p>
        <p className="profile-options-text" onClick={this.onClickSignOut}>Sign Out</p>
      </div>
    )
  }

  renderCreateCirleModal() {
    return (
      <Modal show={this.state.createCircle}
             noPadding={true}
             closeModal={() => this.setState({createCircle: true})}
             width={'true'}
             widthSize={'594px'}
             height={!this.state.addHeight ? '300px' : ''}
             type={!this.state.addHeight ? '' : "create-circle"}
             clickInput={this.state.clickInput}>
             <div>
               {this.state.circleLoading ?
               <div className="spinner-container">
                 <Spinner color="#8136ec" size="150px" margin="100px"/>
               </div>
              :
               <EditCircle
                 type="create-circle"
                 closeEditModal={() => !this.state.notSelected ? this.setState({createCircle: false}) : console.log("not selected")}
                 saveEditedCircle={this.onClickSaveCircle}
                 saveEditedCircleWithPhoto={this.saveEditedCircleWithPhoto}
                 clickInput={(val) => this.setState({clickInput: val})}
                 onClickCloseTip={() => this.setState({addHeight: true})}/>
             }
             </div>
      </Modal>
    )
  }

  renderNotifications(){
    return(
      <Notifications
        notifications={this.state.notifications}
        closeModal={() => this.setState({showNotification:false})}
        showGroupJoiner={this.showGroupJoiner}
        />
    )
  }

  renderProfileView(){
    return(
      <Modal show={this.state.profileOption}
             width={true}
             closeModal={() => this.setState({profileOption: false})}
             height={'350px'}>
            <Profile closePeofileOption={() => this.setState({profileOption: false})}/>
      </Modal>
    )
  }

  renderAddMembersView(){
    return(
      <InviteFriends
        onClickClose={this.onClickCloseShare}
        currentUser={this.props.currentUser}
        memberModalVisible={true}
        groupId={this.state.circle.uuid}
        members={[]}
        fromCreateCircle={true}/>
    )
  }

  renderGroupJoinerView() {
    return(
      <Modal show={this.state.showGroupJoiner}
             closeModal={() => this.setState({showGroupJoiner:false})}>
        <button className="x-btn photo-modal"
          onClick={() => this.setState({showGroupJoiner:false})}>
          <img className="group-joiner-close" alt="close" src={require('../../../assets/images/close-btn-black@2x.png')} />
        </button>
        <div className="group-joiner-container text-center">
          <img src={this.state.groupJoiner.icon ? this.state.groupJoiner.icon : require('../../../assets/images/placeholder.png')}
               className={this.state.groupJoiner.icon ? "group-joiner-photo" : "group-joiner-photo-placeholder"}
               alt={this.getGroupJoinerName(this.state.groupJoiner)}/>
          <p className="group-joiner-name">{this.getGroupJoinerName(this.state.groupJoiner)}</p>
        </div>
      </Modal>
    )
  }


  render() {
    let photo = "";

    if(!_.isEmpty(this.state.currentUser)){
      photo=this.state.currentUser.photo ? this.state.currentUser.photo : require('../../../assets/images/profile-photo-placeholder.png');
    }

    return (
        <nav className="tab-bar" >
            {this.state.profileOption ? this.renderProfileView() : <span/>}
          <section className="left tab-bar-section" >
            <img
              onClick={()=>this.context.router.push('/app')}
              className="nav-logo pointer"
              src={require('../../../assets/images/present-logo.svg')}
              alt="present"/>
          </section>

          <section className="right tab-bar-section">
            {this.state.showSearch ?
              <SearchBox
                closeModal={() => this.setState({showSearch:false})}
                nearByGroups={this.props.nearByGroups}
                onSendQuery={this.onSendQuery}
                searchResult={(value) => this.props.searchResult(value)}/>
              :
              this.props.searchVisible ?
                <button className="profile-btn" onClick={() => this.setState({showSearch:true})}>
                  <img src={require('../../../assets/images/search.svg')} className="navbar-img" alt="search"/>
                </button> :
                <div/>
            }
            <button className="profile-btn"  onClick={() => {this.setState({createCircle:!this.state.createCircle})}}>
              <img
                src={require('../../../assets/images/plus.svg')}
                className="navbar-img"
                alt="add"/>
            </button>
            <button className="profile-btn" onClick={() => {this.setState({showNotification:!this.state.showNotification})}}>
              <img
                src={require('../../../assets/images/notifications.svg')}
                className="navbar-img"
                alt="notifications"/>
            </button>

              <button className="profile-btn position-relative" onClick={() => this.setState({showProfileOptions:!this.state.showProfileOptions})}>
                <img src={photo} className="profile-img" alt="profile" />
              </button>

              {this.state.showNotification ? this.renderNotifications() : <span/>}
              {this.state.showGroupJoiner ? this.renderGroupJoinerView() :  <span/>}
              {this.state.createCircle ? this.renderCreateCirleModal() : <span/>}
              {this.state.showAddMembers ? this.renderAddMembersView() :  <span/>}
              {this.state.showProfileOptions ? this.showProfileOptions() : <span/>}
            {this.props.userId && !_.isEmpty(this.state.currentUser) ? <UserProfile currentUser={this.state.currentUser} userId={this.props.userId} /> : <span/>}
          </section>
          <img src={require('../../../assets/images/divider@3x.png')} className="navbar-divider" alt="divider"/>
        </nav>
    );
  }
}

Navbar.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
    return {
        currentUser: state.user.currentUser,
        notifications:state.activities.notifications,
        inbox: state.activities.inbox,
        savedGroups:state.groups.savedGroups
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({...userActions,...activityActions,...groupsActions,...groupActions,...commentActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(Navbar);
