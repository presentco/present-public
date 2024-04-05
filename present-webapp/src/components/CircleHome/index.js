import React, { Component,PropTypes } from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../redux/actions/userActions';
import * as groupActions from '../../redux/actions/groupActions';
import * as groupsActions from '../../redux/actions/groupsActions';
import * as commentActions from '../../redux/actions/commentActions';
import ApiHandler from '../../core/ApiHandler';
import Constants from '../../core/Constants';
import './style.css';
import _ from 'lodash';
import ChatComponent from './components/chatComponent';
import GeneralComponent from './components/GeneralComponent';
import Navbar from '../common/Navbar';
import GetTheApp from '../common/GetTheApp';
import SavedCircles from '../common/SavedCircles';
import TopBar from './components/chatComponent/Components/TopBar';
import groupUtiles from '../../core/groupUtiles';
import Spinner from 'halogen/RingLoader';
import Modal from './components/Modal';


class CircleHome extends Component {
  constructor(props){
    super(props);
    this.state={
      currentUser: {},
      currentGroup: props.currentGroup ? props.currentGroup : {},
      savedGroups: props.savedGroups ? props.savedGroups : [],
      nearByGroups: props.nearByGroups ? props.nearByGroups : [],
      comments:props.comments ? props.comments : [],
      vanityId:props.vanityId,
      groupId:props.groupId,
      groupActive:"",
      showMobilecomponent:false,
      hideInfo:true
    }

  }

  //Called any time the Props have Changed in the Redux Store
  componentWillReceiveProps(nextProps) {
    //Check if the Props for group have in fact changed.
    if (this.props.comments !== nextProps.comments) {
        this.setState({comments: nextProps.comments });
    }

    if (this.props.currentUser !== nextProps.currentUser) {
        this.setState({currentUser: nextProps.currentUser });
    }

    if (this.props.nearByGroups !== nextProps.nearByGroups) {
        this.setState({nearByGroups: nextProps.nearByGroups });
    }

    if (this.props.savedGroups !== nextProps.savedGroups) {
        this.setState({savedGroups: nextProps.savedGroups });
    }
  }

  componentDidMount(){

    //window.onpopstate = this.handleBackButton.bind(this);

    this.loadUserProfileFromBackend();
    this.loadRequestedGroupFromBackend(this.props.vanityId, "fromDiscovery");
  }

  // handleBackButton(e){
  //   e.preventDefault();
  //   browserHistory.goBack();
  // }


  loadUserProfileFromBackend(){
    let request = ApiHandler.attachHeader({});
    this.props.actions.loadCurrentUser(request).then(response => {
      this.setState({currentUser:response});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  onClickGoToGroup(group){
    this.setState({currentGroup:group,groupActive:group.id});
    this.context.router.push(`/g/${this.getGroupUrl(group)}`);
    this.loadRequestedGroupFromBackend(this.getGroupUrl(group));

  }

  loadRequestedGroupFromBackend(url, fromRoute){

    let newUrl = url;

    if(fromRoute && fromRoute !== "markRead"){
      newUrl = process.env.NODE_ENV !== 'production' ? `${Constants.circleUrl.url}g/${url}` : `${Constants.circleUrl.url}g/${url}`
      this.setState({showSpinner:true})
    }

    let request = ApiHandler.attachHeader({url:newUrl});
      this.props.actions.getGroupInfo(request).then(response => {

        window.history.pushState('','',`/g/${response.url.split('g/')[1]}`);
        this.setState({currentGroup: response, groupId: response.uuid, showSpinner:false});
        if(response.discoverable){
          //IF IT IS PUBLIC OR USER IS A MEMBER
          if(response.preapprove === 'ANYONE' || response.membershipState === 'ACTIVE'){
            if(fromRoute !== "markRead"){
                this.loadGroupComments(response.uuid);
            }
          }
        } else {
          if(response.membershipState === 'ACTIVE'){
            if(fromRoute !== "markRead"){
                this.loadGroupComments(response.uuid);
            }
          }
        }
      }).catch(error => {
      
        const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
        if(error.response && error.response.data && error.response.data.error.message === "Unauthorized"){
          this.setState({showError:true,showSpinner:false });
        }
      });
  }



  markRead(request){
    let that = this;
    this.props.actions.markRead(request).then(res => {
      that.loadUserSavedGroups();
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  loadUserSavedGroups(){
    let home = !_.isEmpty(this.state.currentUser) ? this.state.currentUser.home.location : {};
    let requestTest = ApiHandler.attachHeader({
      location: home
    });

    this.props.actions.getSavedGroups(requestTest).then( response => {
      this.setState({
        savedGroups: response.groups
      });
    }).catch(error => {

      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  loadGroupComments(groupId){
    let request = ApiHandler.attachHeader({groupId:groupId}),
        that = this;
    this.props.actions.loadRequestedGroupChats(request).then( response => {
      if(response){
        this.setState({comments: response});
      }

      if(that.state.currentGroup.unread){

          let request = ApiHandler.attachHeader({
            groupId: groupId,
            lastRead : response.length > 0 ? response[response.length - 1].index : -1
          });

          this.markRead(request);

      }
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  render() {
    let showSpinner,showError  = <span/>;

    if(this.state.showSpinner){
      showSpinner = (
        <Modal show={this.state.showSpinner}
               noPadding={true}
               noBoxShadow={true}
               closeModal={() => this.setState({showSpinner: false})}>
          <div className="margin-top-1" ref="myRef">
              <Spinner color="#8136ec" size="70px" margin="20px"/>
          </div>
        </Modal>
      )
    }

    if(this.state.showError){
      showError = (
        <Modal show={this.state.showError}
               closeModal={() => this.setState({showError: false})}>
                <div className="text-center">
                  <p className="error-got-text">You don't have permission to see this circle!</p>
                  <button className="error-got-btn" onClick={() => this.context.router.push('/app')}>Got it, take me back to discover more circles!</button>
               </div>
        </Modal>
      )
    }

    return (
      <div>
        {showSpinner}
        {showError}
        <Navbar loadUserProfileFromBackend={() => this.loadUserProfileFromBackend()}/>
        <div>
          <div className="small-12 large-2 gray-bg on-circle no-padding show-for-large-up columns">
            <GetTheApp currentUser={this.props.currentUser}/>
              <div className="city-container pointer" onClick={() => this.context.router.push('/app')}>
                <p className="city-text">{groupUtiles.getSelectedSpace()}</p>
              </div>
            <div className="city-container pointer" onClick={() => this.context.router.push('/app')}>
              <p className="city-text">{groupUtiles.getSelectedCity(!_.isEmpty(this.props.currentUser) ? this.props.currentUser.home : "").name}</p>
            </div>
            <SavedCircles
              type="circle-home"
              savedGroups={this.state.savedGroups}
              fromRoute="circleHome"
              loadRequestedGroupFromBackend={(val) => this.loadRequestedGroupFromBackend(val)}/>
          </div>
          <div className="hide-for-small-only">
            <div className="small-12 medium-8 large-7 columns gray-bg no-padding">
              <ChatComponent
                  {...this.props}
                  groupId={this.state.currentGroup.uuid}
                  savedGroups={this.state.savedGroups}
                  checkConnection={this.checkConnection}
                  socket={this.connection}
                  callJoinButton={() => this.loadRequestedGroupFromBackend(this.props.currentGroup.url)}
                  />
              </div>

              <div className="small-12 medium-4 large-3 columns no-padding-left gray-bg">
                <GeneralComponent
                  {...this.props}
                  getCurrentGroup={() => this.loadRequestedGroupFromBackend(this.props.currentGroup.url)}
                  currentGroup={this.state.currentGroup}
                  groupId={this.state.currentGroup.uuid}
                  savedGroups={this.state.savedGroups}
                  media={groupUtiles.getAllChatMedia(this.state.comments)}
                  />
              </div>
            </div>

            <div className="show-for-small-only">
              {this.state.hideInfo ?
                <ChatComponent
                  {...this.props}
                  groupId={this.state.currentGroup.uuid}
                  savedGroups={this.state.savedGroups}
                  checkConnection={this.checkConnection}
                  socket={this.connection}
                  hideInfo={() => this.setState({hideInfo: !this.state.hideInfo})}
                  />
                :
                <div className="small-12 small-centered columns no-padding-left gray-bg">
                  <div className="top-bar-container">
                    <TopBar
                      {...this.props}
                      currentGroup={this.state.currentGroup}
                      hideInfo={() => this.setState({hideInfo: !this.state.hideInfo})}
                      groupId={this.state.currentGroup.uuid}
                      savedGroups={this.state.savedGroups}
                      currentUser={this.state.currentUser}/>
                  </div>
                  <GeneralComponent
                    {...this.props}
                    getCurrentGroup={() => this.loadRequestedGroupFromBackend(this.props.currentGroup.url)}
                    currentGroup={this.state.currentGroup}
                    groupId={this.state.currentGroup.uuid}
                    savedGroups={this.state.savedGroups}
                    media={groupUtiles.getAllChatMedia(this.state.comments)}
                    />
                </div>
              }
            </div>

        </div>
      </div>
    );
  }
}

CircleHome.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
    let vanityId = ownProps.params.vanityId;
    return {
        vanityId: vanityId,
        groupId: state.group.groupId,
        currentUser: state.user.currentUser,
        comments: state.comments.comments,
        savedGroups: state.groups.savedGroups,
        currentGroup: state.group.currentGroup,
        nearByGroups: state.groups.nearByGroups
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({...userActions,...groupActions, ...commentActions,...groupsActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(CircleHome);
