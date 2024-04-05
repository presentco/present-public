import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as groupActions from '../../redux/actions/groupActions';
import * as groupsActions from '../../redux/actions/groupsActions';
import * as userActions from '../../redux/actions/userActions';
import Constants from '../../core/Constants';
import groupUtiles from '../../core/groupUtiles';
import ApiHandler from '../../core/ApiHandler';
import Modal from '../CircleHome/components/Modal';
import Navbar from '../common/Navbar';
import NamePhoto from './Components/NamePhoto';
import UserBio from './Components/UserBio';
import UserCommunity from './Components/UserCommunity';
import Cards from '../Discovery/Components/Cards';
import _ from 'lodash';
import './style.css';
import CopyToClipboard from 'react-copy-to-clipboard';

import {
  ShareButtons
} from 'react-share';

const {
  FacebookShareButton,
  TwitterShareButton
} = ShareButtons;

export class UserProfile extends Component {
  constructor(props) {
    super(props);

    this.state ={
      user:{},
      savedGroups:{},
      createdCircles:[],
      circleType:'created',
      userId: props.userId,
      currentUser: props.currentUser ? props.currentUser : {},
      validStateTransitions:[],
      myFriends:[],
      userFriends:[],
      friendsRequests:[],
      myIncomingFriendsRequests:[],
      newFacebookFriends:[],
      joined:false
    }

    this.onClickBlock = this.onClickBlock.bind(this);
    this.onClickJoin = this.onClickJoin.bind(this);
    this.onChangeSearchFriend = this.onChangeSearchFriend.bind(this);
    this.loadRequestedUserFromUrl();
    this.getMyFriends();

  }


  componentWillReceiveProps(nextProps){
    if (this.props.currentUser !== nextProps.currentUser) {
      let url = window.location.href;

      if(window.location.href.indexOf('local') > -1){
        let newUrl = window.location.href.split('3000')[1];
        url = `https://staging.present.co${newUrl}`
      }

      this.setState({showFacebookBtn: nextProps.currentUser.facebookLinked})
      if(nextProps.currentUser.link === url){
        this.setState({user: nextProps.currentUser});

        this.getFacebookFriends();
      }
    }
  }


  getIncomingFriendRequests(){
    let request= ApiHandler.attachHeader({});

    this.props.actions.getIncomingFriendRequests(request).then(response => {
      this.setState({myIncomingFriendsRequests:response});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }


  getOutcomingFriendRequests(){
    let request= ApiHandler.attachHeader({});
    this.props.actions.getOutgoingFriendRequests(request).then(response => {

      this.setState({myOutgoingFriendsRequests:response});
      this.getFriendshipStatus(response, 'Requested');
    }).catch(error => {

      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  getFacebookFriends(){
    let request = ApiHandler.attachHeader({});

    this.props.actions.getFacebookFriends(request).then(response => {
      this.setState({facebookFriends: response});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }


  onChangeSearchFriend(e){

    e.preventDefault();
    this.setState({searchFriend: e.target.value});
    let newFacebookFriends = [];

    if(e.target.value.length > 0){
      for(let user of this.state.facebookFriends){
        if(user.name.toLowerCase().indexOf(e.target.value) > -1){
          newFacebookFriends.push(user);
        }
      }
      this.setState({newFacebookFriends});
    } else {
      this.setState({newFacebookFriends: []});
    }
  }

  getMyFriends(){
    let request = ApiHandler.attachHeader({});

    this.props.actions.getFriends(request).then(response => {

      this.setState({myFriends:response});
      this.getOutcomingFriendRequests();
      this.getIncomingFriendRequests();

    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  getUserFriends(userId){
    let request = ApiHandler.attachHeader({userId}),
        that = this;

    this.props.actions.getFriends(request).then(response => {
      this.setState({userFriends:response});

      that.getFriendshipStatus(response, 'Friends');
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  getFriendshipStatus(lists,type){
    if(lists.length > 0){
        for(let list of lists){
          if(type === 'Requested'){
            if(list.id === this.state.user.id){
                this.setState({friendStatus: 'Requested', joined:true});
                break;
              } else {
                  this.setState({friendStatus: 'Add', joined:false});
              }
          } else if(type === 'Friends'){
              if(list.id === this.props.currentUser.id){
                this.setState({friendStatus: 'Added', joined:true});
                break;
              } else {
                  this.setState({friendStatus: 'Add', joined:false});
              }
          }
          }
    }
  }

  getValidStateTransitions(userId){
    let request = ApiHandler.attachHeader({userId})
    this.props.actions.getValidStateTransitions(request).then(response => {
      this.setState({
        validStateTransitions: response.data.result.validStateTransitions
      })
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }


  onClickBlock(){
    let request = ApiHandler.attachHeader({userId: this.state.user.id})
    this.props.actions.blockUser(request).then(response => {
      this.setState({openBurgerBtn:false})
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  loadRequestedSavedGroups(){
    let request = ApiHandler.attachHeader({userId:this.state.user.id}),
        circleType = this.state.circleType,
        that = this;
      this.props.actions.getOtherUserSavedGroups(request).then(response => {
        if(groupUtiles.getUserCreatedCircles(that.state.user, response.groups).length === 0){
          if(groupUtiles.getUserOnlyJoinedCircles(that.state.user, response.groups).length !== 0){
            circleType = 'joined';
          }
        }

        this.setState({
          showSavedCircles:true,
          savedGroups: groupUtiles.getUserOnlyJoinedCircles(this.state.user, response.groups),
          createdCircles: groupUtiles.getUserCreatedCircles(this.state.user, response.groups),
          circleType:circleType
        });
      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
      });

  }

  onClickJoin(){
    let addText = this.state.joined ? "Add" : "Requested";
    this.setState({joined: !this.state.joined, friendStatus: addText});
    if(!this.state.joined){
      this.addFriend(this.state.user.id);
    } else{
      this.removeFriend(this.state.user.id);
    }
  }


  getRequestedMember(member){
    this.context.router.push(`/u/${member.link.split('/u/')[1]}`);
    this.loadRequestedUserFromUrl(member,'trusted');
    this.setState({openMembersModal:false, openFriendRequestsModal:false, openAddFriendModal:false});
  }

  addFriend(userId){
    let request = ApiHandler.attachHeader({userId}),
        that = this,
        title = "";

    that.props.actions.addFriend(request).then(response => {

      that.getIncomingFriendRequests();
      that.getOutcomingFriendRequests();
      title = _.startCase(_.toLower(response.result));
      if(title === 'Accepted'){
        title = "Joined";
      }

      that.setState({friendStatus:title});
    }).catch(error => {

      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  removeFriend(userId){
    let request = ApiHandler.attachHeader({userId}),
        that = this;

    that.props.actions.removeFriend(request).then(response => {
      that.getMyFriends();
      that.getIncomingFriendRequests();
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  isUserMyFriend(userId){

    let myFriends = this.state.myFriends,
        isTrue;

    for(let friend of myFriends){
      if(friend.id === userId){
        isTrue = true;
        break;
      }
    }

    return isTrue;
  }

  loadRequestedUserFromUrl(user, trusted){

    let newUrl,
        that = this,
        request;
    if(user === 'fromNavbar') {
      this.setState({user:this.props.currentUser});
      this.getMyFriends();
      this.loadRequestedSavedGroups();
      window.history.pushState('','',`/u/${this.props.currentUser.link.split('u/')[1]}`);
      } else {
        if(typeof user === 'object' && trusted === 'trusted'){
          newUrl = user.link;
        }  else {
          newUrl = `${Constants.circleUrl.url}u/${this.props.userId}`;
        }

         request = ApiHandler.attachHeader({url:newUrl});

          this.props.actions.getGroupInfo(request).then(response => {
            window.history.pushState('','',`/u/${response.link.split('u/')[1]}`);

            if(that.props.currentUser.id === response.id){
                this.setState({user: that.props.currentUser});
            } else {
              that.setState({user: response});

              if(that.props.currentUser.isAdmin || (!_.isEmpty(that.state.myFriends) && that.isUserMyFriend(response.id))){

                that.getUserFriends(response.id);
              } else {
                this.setState({userFriends: []});
                this.getOutcomingFriendRequests();
              }

            }

            this.loadRequestedSavedGroups();

            if(response.isAdmin){
              this.getValidStateTransitions(response.id);
            }

          }).catch(error => {
            const errorMessage = ApiHandler.getErrorMessage(error);
            console.log(errorMessage);
          });
      }

  }

  isMemberAFriend(memberId){
    let isFriend = false;
    if(this.state.myFriends.length > 0){
      for(let friend of this.state.myFriends){
        if(friend.id === memberId){
          isFriend = true;
          break;
        }
      }
    }

    return isFriend;
  }

  isMemberRequestedFriend(memberId){
    let isFriendRequest = false;

    if(this.state.myOutgoingFriendsRequests.length > 0){
      for(let request of this.state.myOutgoingFriendsRequests){
        if(request.id ===  memberId){
          isFriendRequest = true;
          break;
        }
      }
    }

    return isFriendRequest;
  }

  renderShareOptions(){

    let link = `mailto:yourfriend@example.com?Subject=Join%20me%20on%20Present!%20&body=Hi%20friends!%20Join%20me%20on%20Present!%20%0D%0A%0D%0A${this.state.user.link}`,
        quote = `Hi friends! Join me on Present! ${this.state.user.link}`;

    return(
      <Modal show={this.state.shareClicked}
             closeModal={() => this.setState({shareClicked:false})}>
            <div className="click-share-container">
                <div className="where-toshare-container">
                  <p className="choose-share">Choose where to share</p>
                  <div className="share-win-container">
                    <CopyToClipboard text={window.location.href}
                      onCopy={(text) => this.setState({linkCopied: true, linkText: text, shareClicked:false })}>
                      <div className="text-center cursor-poiner">
                        <img alt="link" className="icon-link" src={require('../../assets/images/link@2x.png')}/>
                        <p className="icon-title">Link</p>
                      </div>
                    </CopyToClipboard>
                    <div className="text-center cursor-poiner" onClick={this.shareToFb}>
                      <FacebookShareButton
                       url={this.state.user.link} quote={quote}>
                         <img alt="facebook" className="icon-link" src={require('../../assets/images/facebook-link@2x.png')}/>
                         <p className="icon-title">Facebook</p>
                     </FacebookShareButton>
                    </div>
                    <div className="twitter-email-div">
                      <a href={link} className="text-center cursor-poiner">
                        <img alt="email" className="icon-link" src={require('../../assets/images/email-link@2x.png')}/>
                        <p className="icon-title">Email</p>
                      </a>
                    </div>
                    <div className="text-center cursor-poiner">
                      <TwitterShareButton
                       url={this.state.user.link}
                       title={`Hi friends! Join me on Present!`}>
                       <img alt="twitter" className="icon-link" src={require('../../assets/images/twitter-link@2x.png')}/>
                       <p className="icon-title">Twitter</p>
                     </TwitterShareButton>
                    </div>
                  </div>
                </div>
           </div>
      </Modal>
    )
  }

  renderLinkshared(){
    return(
      <Modal show={this.state.linkCopied}
             closeModal={() => this.setState({linkCopied:false})}>
             <img src={require('../../assets/images/close-btn-white@2x.png')}
            className="white-x"
            alt="close"
            onClick={() => this.setState({linkCopied:false})}/>
           <div className="confrim-container text-center ">
             <p className="confirm-text is-success">Copied link to clipboard</p>
             <p className="info-text">Share your profile with friends!</p>
             <p>{this.state.linkText}</p>
           </div>
      </Modal>
    )
  }

  renderMyIncomingRequests(){
    let that = this;
    return(
      this.state.myIncomingFriendsRequests.map((request, index) => {
        return(
          that.props.currentUser.id !== request.id ?
            <div key={index} className="owner-container position-relative">
            <img
              src={request.photo}
              className="owner-photo cursor-poiner"
              onClick={this.getRequestedMember.bind(this, request)}
              alt={request.firstName}/>
            <p className="cursor-poiner"
              onClick={this.getRequestedMember.bind(this, request)}>{request.firstName}</p>
              <div className="approve-container">
                <button className="approve-btn" onClick={this.addFriend.bind(this,request.id)}>Add</button>
                <img src={require('../../assets/images/close-btn-black@2x.png')}
                  className="close-member cursor-poiner"
                  alt="remove"
                  onClick={this.removeFriend.bind(this,request.id)} />
              </div>
          </div>
          : <span/>
        )
      })
    )
  }

  renderMyFriendsList(friends, type){
    if(this.state.newFacebookFriends.length !== 0){
      friends = this.state.newFacebookFriends;
    }

    return(
      friends.map((request, index) => {
        return(
          <div key={index} className="owner-container position-relative">
            <img src={request.photo}
              className="owner-photo cursor-poiner"
              onClick={this.getRequestedMember.bind(this, request)}
              alt={request.firstName}/>
            <p className="cursor-poiner"
              onClick={this.getRequestedMember.bind(this, request)}>{request.name}</p>
            {this.props.currentUser.id === this.state.user.id && type !== 'facebook' ?
              <div className="approve-container">
                <img src={require('../../assets/images/close-btn-black@2x.png')}
                  className="close-member cursor-poiner"
                  onClick={this.removeFriend.bind(this,request.id)}
                  alt="remove" />
              </div>
              :
            <span/>}
              {type === 'facebook' ?
                this.isMemberAFriend(request.id) ?
                  <div className="approve-container">
                    <button className="approved-btn add-btn">Added</button>
                  </div> :
                  this.isMemberRequestedFriend(request.id) ?
                  <div className="approve-container">
                    <button className="approved-btn">Requested</button>
                  </div> :
                  <div className="approve-container">
                    <button className="approve-btn add-friend-btn" onClick={this.addFriend.bind(this,request.id)}>Add</button>
                  </div>
                  : <span/>
                }
          </div>
        )
      })
    )
  }

  isNotCurrentUser(){
    let isNotCurrentUser = this.state.myIncomingFriendsRequests.length;
    if(this.state.myIncomingFriendsRequests.length > 0){
      for(let request of this.state.myIncomingFriendsRequests){
        if(request.id === this.props.currentUser.id){
          isNotCurrentUser = this.state.myIncomingFriendsRequests.length - 1;
          break;
        }
      }
    }

    return isNotCurrentUser;
  }

  renderFriendRequestsModal(){
    return(
      <Modal
        show={this.state.openFriendRequestsModal}
        closeModal={() => this.setState({openFriendRequestsModal:false})}
        noPadding={true}
        width={'true'}>
        <div className="member-navbar">
          <img src={require('../../assets/images/back-btn-black.svg')}
            alt="back"
            className="back-btn-black left back-member-btn"
            onClick={() => this.setState({openFriendRequestsModal:false})}/>
          <p className="middle members-title">Friends</p>
        </div>
        <div className="creator-container">
          <span>{this.isNotCurrentUser()} Friend Requests</span>
        </div>
        {this.state.myIncomingFriendsRequests.length > 0  ?
          this.renderMyIncomingRequests()
          : <span/>}

      </Modal>
    )
  }

  renderAllFriends(){
    return(
      <Modal
        show={this.state.openMembersModal}
        closeModal={() => this.setState({openMembersModal:false})}
        noPadding={true}
        width={'true'}>
        <div className="member-navbar">
          <img alt="back" src={require('../../assets/images/back-btn-black.svg')}
            className="back-btn-black left back-member-btn"
            onClick={() => this.setState({openMembersModal:false})}/>
          <p className="middle members-title">Friends</p>
        </div>
        <div className="creator-container">
          <span>{this.props.currentUser.id === this.state.user.id ? this.state.myFriends.length : this.state.userFriends.length} Friends</span>
        </div>
        {this.props.currentUser.id === this.state.user.id ?
          this.state.myFriends.length > 0 ?
          this.renderMyFriendsList(this.state.myFriends)
          : <span/> :
        this.state.userFriends.length > 0 ?
          this.renderMyFriendsList(this.state.userFriends) :
          <span/>
      }
      </Modal>
    )
  }

  renderFriendModal(){
    return(
      <Modal
        show={this.state.openAddFriendModal}
        closeModal={() => this.setState({openAddFriendModal:false})}
        noPadding={true}
        width={'true'}>
        <div className="member-navbar add-border-bottom">
          <img alt="back" src={require('../../assets/images/back-btn-black.svg')}
            className="back-btn-black left back-member-btn"
            onClick={() => this.setState({openAddFriendModal:false})}/>
          <p className="middle members-title">Add Friends</p>
          <div className="search-friends-container">
            <img calssName="search-icon-friends" src={require('../../assets/images/search@2x.png')} alt="search"/>
            <input
              className="search-input serach-friend-input"
              value={this.state.searchFriend}
              placeholder="Search for a friend"
              onChange={this.onChangeSearchFriend}/>
            <p className="cancle-search-friend-btn" onClick={() => this.setState({searchFriend: '', newFacebookFriends:[]})}>Cancel</p>
          </div>
        </div>
        <div className="gray-title-divider">
          <span>Facebook Friends on Present</span>
        </div>
        {this.state.facebookFriends.length > 0 ?
          this.renderMyFriendsList(this.state.facebookFriends, 'facebook')
          : <span/>}
      </Modal>
    )
  }

  render() {

    let savedLength = !_.isEmpty(this.state.savedGroups) ? this.state.savedGroups.length : "",
        createdLength = !_.isEmpty(this.state.savedGroups) ? this.state.createdCircles.length : "",
        user = this.state.user,
        isCurrentUser = !_.isEmpty(this.props.currentUser) && this.state.user.id === this.props.currentUser.id,
        joinImg = !this.state.joined ? 'join@2x.png' : 'pink-joined.png';

    return (
      <div>
      <Navbar loadRequestedUserFromUrl={() => this.loadRequestedUserFromUrl('fromNavbar')} />
        <div className="padding-1-top">
          {this.state.openMembersModal ? this.renderAllFriends() : <span/>}
          {this.state.openFriendRequestsModal ? this.renderFriendRequestsModal() : <span/>}
          {this.state.openAddFriendModal ? this.renderFriendModal() : <span/>}
          <div className="small-5 medium-3 medium-uncentered large-3 padding-bottom-row large-uncentered scrollable columns">
            <NamePhoto
              user={this.state.user}
              currentUser={this.props.currentUser}
              validStateTransitions={this.state.validStateTransitions}/>


            {/*isCurrentUser && !this.state.showFacebookBtn && window.FB ?
            <FacebookLoginButton
              type="user-profile"
              vanityId={this.props.vanityId}
              onCloseFacebook={() => this.setState({showFacebookBtn: true})}
              />
            : <span/>*/}

              <div className={"user-photo-contrainer user-option-container"}>
              {this.state.shareClicked ?  this.renderShareOptions() : <span/>}
              {this.state.linkCopied ? this.renderLinkshared() : <span/>}
              {!isCurrentUser ?
                <div className="cursor-pointer share-option-div" onClick={this.onClickJoin}>
                  <img src={require(`../../assets/images/${joinImg}`)} alt={user.firstName} className="general-icon"/>
                  <span className="option-title">{!this.state.joined ? "Add" : this.state.friendStatus}</span>
                </div> : <span/>}

                <div className={"cursor-pointer share-option-div"} onClick={() => this.setState({shareClicked: true})}>
                  <img src={require('../../assets/images/share-profile@2x.png')} alt={user.firstName} className="general-icon"/>
                  <span className="option-title">Share Profile</span>
                </div>
                {isCurrentUser ?
                  <div className="cursor-pointer share-option-div" onClick={() => this.setState({openAddFriendModal: true})}>
                    <img src={require(`../../assets/images/invite-friends@2x.png`)} alt={user.firstName} className="general-icon"/>
                    <span className="option-title">Add Friends</span>
                  </div> : <span/>}

              </div>

            {!_.isEmpty(this.state.user) && this.state.user.bio && this.state.user.bio.length > 0 ? <UserBio user={this.state.user} /> : <span/>}
            <UserCommunity
              user={this.state.user}
              loadRequestedUserFromUrl={(user) => this.loadRequestedUserFromUrl(user, 'trusted')}
              openCommunityModal={() => this.setState({openMembersModal:true})}
              currentUser={this.props.currentUser}
              customIncomingFriendsRequests={this.isNotCurrentUser()}
              myIncomingFriendsRequests={this.state.myIncomingFriendsRequests}
              openFriendRequestsModal={() => this.setState({openFriendRequestsModal: true})}
              type={!_.isEmpty(this.props.currentUser) && !_.isEmpty(this.state.user) && this.props.currentUser.id === this.state.user.id ? "my-friends" : "user-friends"}
              myFriends={ !_.isEmpty(this.props.currentUser) && !_.isEmpty(this.state.user) && this.props.currentUser.id === this.state.user.id ? this.state.myFriends : this.state.userFriends}/>
            {/*<UserInterests user={this.state.user} />*/}
          </div>
          <div className="small-7 medium-9 medium-uncentered no-padding-left large-9 gray-bg large-uncentered columns">
            <div className="row white-bg no-padding border-radius-5">
              <div className="small-6 medium-6 large-6 columns text-center">
                <p className="circle-titles cursor-pointer" onClick={() => this.setState({circleType: 'created'})}>
                  Created Circles <span className="saved-length">{createdLength}</span>
                </p>
              </div>
              <div className="small-6 medium-6 large-6 columns text-center">
                <p className="circle-titles cursor-pointer" onClick={() => this.setState({circleType: 'joined'})}>
                  Joined Circles <span className="saved-length">{savedLength}</span>
                </p>
              </div>
            </div>
            {!_.isEmpty(this.state.savedGroups) ?
              <div className="padding-vertical">
                {this.state.circleType === 'joined' ?
                    <div className="cards-container">
                      <Cards nearByGroups={this.state.savedGroups} type="user-profile"/>
                    </div>
                   : <div className="cards-container">
                       <Cards nearByGroups={this.state.createdCircles} type="user-profile"/>
                     </div>
                  }
              </div>
                 :
                 !_.isEmpty(this.state.user) && this.state.user.id === this.props.currentUser.id ?
                 <p className="text-center padding-top">You dont have any saved circles</p> :
                 <p className="text-center padding-top">No saved circles!</p>
             }
            </div>
      </div>
    </div>
    );
  }
}

UserProfile.contextTypes = {
  router: PropTypes.object
};

function mapStateToProps(state, ownProps) {
  const userId = ownProps.params.userId;
  return {
    userId:userId,
    currentUser: state.user.currentUser,
    savedGroups: state.groups.savedGroups
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({...groupsActions,...userActions,...groupActions}, dispatch)
  };
}

export default connect(mapStateToProps,mapDispatchToProps)(UserProfile);
