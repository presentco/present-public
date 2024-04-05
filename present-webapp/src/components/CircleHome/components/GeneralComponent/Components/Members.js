import React, { Component, PropTypes} from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import ApiHandler from '../../../../../core/ApiHandler';
import * as groupActions from '../../../../../redux/actions/groupActions';
import * as userActions from '../../../../../redux/actions/userActions';
import UserDetails from '../../../../common/UserDetailsModal';
import Modal from '../../Modal';
import userUtiles from '../../../../../core/userUtiles';
import _ from 'lodash';

class Members extends Component {

  constructor(props){
    super(props);
    this.state={
      currentGroup:{},
      members:props.members,
      showMemberModal:false,
      requests:props.memberRequests ? props.memberRequests : [],
      addUserIds:[],
      removeUserIds:[],
      myFriends:[],
      myOutgoingFriendsRequests:[],
      title:''
    }

    this.onClickSaveMemberRequests = this.onClickSaveMemberRequests.bind(this);

  }

  //Called any time the Props have Changed in the Redux Store
  componentWillReceiveProps(nextProps) {
    //Check if the Props for group have in fact changed.
    if (this.props.currentGroup !== nextProps.currentGroup) {
        this.setState({currentGroup: nextProps.currentGroup });
        this.getGroupMembers(nextProps.currentGroup.uuid);
        this.getOutgoingFriendRequests();
        this.getFriends();
    }

    if(this.props.memberRequests !== nextProps.memberRequests) {
        this.setState({requests: nextProps.memberRequests });
    }
  }


  getRequestedMember(member){
    this.context.router.push(userUtiles.getUserLink(member));
  }

  getFriends(){
    let request = ApiHandler.attachHeader({userId: this.props.currentUser.id});
    this.props.actions.getFriends(request).then(myFriends => {
      this.setState({myFriends});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  getOutgoingFriendRequests(){
    let request= ApiHandler.attachHeader({});

    this.props.actions.getOutgoingFriendRequests(request).then(response => {

      this.setState({myOutgoingFriendsRequests:response});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  addFriend(userId){
    let request = ApiHandler.attachHeader({userId}),
        that = this;

    that.props.actions.addFriend(request).then(response => {
      that.getOutgoingFriendRequests();
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  addMember(userId){
    let addUserIds = this.state.addUserIds;
      addUserIds.push(userId);
      this.setState({addUserIds});
  }

  callAddMembers(){
    let request = ApiHandler.attachHeader({groupId: this.props.currentGroup.uuid, userIds:this.state.addUserIds});
    this.props.actions.addMembers(request).then(response => {
      this.props.getMembershipRequests(this.props.currentGroup.uuid);
      this.getGroupMembers(this.props.currentGroup.uuid)
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  callRemoveMembers(){
    let request = ApiHandler.attachHeader({groupId: this.props.currentGroup.uuid, userIds:this.state.removeUserIds});
    this.props.actions.removeMembers(request).then(response => {
      this.props.getMembershipRequests(this.props.currentGroup.uuid);
      this.getGroupMembers(this.props.currentGroup.uuid)
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  removeMember(userId){
    let removeUserIds = this.state.removeUserIds;
    removeUserIds.push(userId);
    this.setState({removeUserIds});
  }

  onClickSaveMemberRequests(){
    this.setState({showMemberRequests:false});
    if(this.state.addUserIds.length > 0){
      this.callAddMembers();
    }

    if(this.state.removeUserIds.length > 0){
      this.callRemoveMembers();
    }
  }

  putCreatorFirst(members){
    for(let member of members){
      if(this.props.currentGroup.owner.id === member.id){
        let author = member;
        members.splice(members.indexOf(member) , 1);
        members.splice(0,0,author);
      break;
      }
    }
    return members;
  }

  getGroupMembers(group){

    let request = ApiHandler.attachHeader({groupId: group})
    this.props.actions.getGroupMembers(request).then(response => {
      if(!response.error){

        this.setState({members: this.putCreatorFirst(response.result.members)});
        this.props.getMembers(response.result.members)
      }
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  renderMembers(){
      if(this.state.members.length !== 0){
       return this.state.members.map((member,index) => {
          return(
            <div className="tesssssst" key={index} >
              <div className="member-main-container" onClick={this.getRequestedMember.bind(this,member)}>
                <img src={member.photo}
                     alt={member.name}
                     className="member-img" />
                   <p className="member-name">{member.firstName}</p>
                     {this.state.currentGroup.owner.id === member.id ?
                       <p className="creator">Creator</p> :
                       <span/>
                     }
                </div>
            </div>
          )
      });
    }
  }

isUserApproved(newUserId){
  let addUserIds = this.state.addUserIds,
      isNotApproved = false;

      for(let userId of addUserIds){
        if(userId === newUserId){
          isNotApproved = true;
          break;
        }
      }

      return isNotApproved;
}

isUserNotApproved(newUserId){
  let removeUserIds = this.state.removeUserIds,
      isNotApproved = false;

      for(let userId of removeUserIds){
        if(userId === newUserId){
          isNotApproved = true;
          break;
        }
      }

      return isNotApproved;
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


  renderGetRequestMembers(){
    return(
      this.state.requests.map((request, index) => {
        return(
          <div key={index} className="owner-container position-relative">
            <img alt={request.user.firstName} src={request.user.photo} className="owner-photo cursor-poiner" onClick={this.getRequestedMember.bind(this, request.user)}/>
            <p class="cursor-poiner"
              onClick={this.getRequestedMember.bind(this, request.user.id)}>{request.user.firstName}</p>
            <div>
              {this.isUserApproved(request.user.id) ?
                <div className="approve-container">
                  <button className="approved-btn">Approved</button>
                </div> :
                this.isUserNotApproved(request.user.id) ?
                <div className="not-approve-container">
                  <button className="approved-btn-container">
                    <img className="trash-icon" src={require('../../../../../assets/images/trash@2x.png')} alt="not approve"/>
                  </button>
                </div> :
                <div className="approve-container">
                  <button className="approve-btn" onClick={this.addMember.bind(this,request.user.id)}>Approve</button>
                  <img src={require('../../../../../assets/images/close-btn-black@2x.png')}
                    className="close-member cursor-poiner"
                    alt="remove"
                    onClick={this.removeMember.bind(this,request.user.id)} />
                </div>
              }

            </div>

          </div>
        )
      })
    )
  }

  renderMemberLists(){
    let that = this;

    return(
      that.state.members.map((member,index) => {
        return(
          <div key={index} className="owner-container position-relative cursor-poiner">
            <img alt={member.firstName} src={member.photo} className="owner-photo" onClick={that.getRequestedMember.bind(that, member)}/>
            <p onClick={that.getRequestedMember.bind(that, member)}>{member.firstName}</p>
            {member.id !== that.props.currentUser.id ?
              that.isMemberAFriend(member.id) ?
              <div className="approve-container">
                <button className="approved-btn add-btn">Added</button>
              </div> :
              that.isMemberRequestedFriend(member.id) ?
              <div className="approve-container">
                <button className="approved-btn">Requested</button>
              </div> :
              <div className="approve-container">
                <button className="approve-btn add-friend-btn" onClick={that.addFriend.bind(that,member.id)}>Add</button>
              </div>
             : <span/>}
          </div>
        )
      })
    )
  }

  renderMemberRequests(){
    return(
      <Modal
        show={this.state.showMemberRequests}
        closeModal={() => this.setState({showMemberRequests:false})}
        noPadding={true}
        width={'true'}>
        <div className="member-navbar">
          <img src={require('../../../../../assets/images/back-btn-black.svg')}
            className="back-btn-black left back-member-btn"
            alt="back"
            onClick={() => this.setState({showMemberRequests:false})}/>
          <p className="middle members-title">Members</p>
          <p className="saved-text cursor-poiner right" onClick={this.onClickSaveMemberRequests}>Save</p>
        </div>
        <div className="creator-container">
          <span>Creator</span>
        </div>
        <div className="owner-container">
          <img alt={this.props.currentGroup.owner.firstName}
              src={this.props.currentGroup.owner.photo} className="owner-photo"/>
          <p>{this.props.currentGroup.owner.firstName}</p>
        </div>
        {this.state.requests.length !== 0 ?
          <div className="creator-container">
            <span>{this.state.requests.length} Member Requests</span>
          </div>
        : <span/>}

        {this.renderGetRequestMembers()}
        <div className="creator-container">
          <span>Members</span>
        </div>
        {this.renderMemberLists()}
      </Modal>
    )
  }


  render(){
    return(
      <div className="description-container member-margin">
          <p className="title-gen">{this.props.currentGroup.memberCount} Members</p>
          {!_.isEmpty(this.props.currentUser) &&
            !_.isEmpty(this.props.currentGroup) &&
            (userUtiles.isUserAdmin(this.props.currentUser) || userUtiles.isUserCircleOwner(this.props.currentUser, this.props.currentGroup) || this.props.currentGroup.joined) ?
            <p className="view-all-members"
               onClick={() => this.setState({showMemberRequests: true})}>View All</p> : <span/>}
          <div className="member-container" >
            {!_.isEmpty(this.props.currentUser) &&
              !_.isEmpty(this.props.currentGroup) &&
              this.state.requests.length > 0 &&
              (userUtiles.isUserAdmin(this.props.currentUser) || userUtiles.isUserCircleOwner(this.props.currentUser, this.props.currentGroup) || this.props.currentGroup.joined) ?
            <div className="tesssssst">
              <div className="member-main-container">
                <div className="member-request-number">
                  <span>{this.state.requests.length}</span>
                </div>

                <img src={require('../../../../../assets/images/member-request-icon@2x.png')}
                     alt="requests"
                     className="member-img"
                     onClick={() => this.setState({showMemberRequests: true})}/>
                   <p className="member-name">Requests</p>
                </div>
            </div> : <span/>}
            {this.renderMembers()}
            {this.state.showMemberRequests ? this.renderMemberRequests() :  <span/>}
            {this.state.showMemberModal ?
               <UserDetails
                 currentUser={this.props.currentUser}
                 user={this.state.member}
                 showMemberModal={this.state.showMemberModal}
                 onCloseModal={() => this.setState({showMemberModal:false})}/> : <span/>}
            {this.state.members.length >= 6 ?
              <div className="member-hidden-div"></div>
            :
              <span/>
            }
          </div>
      </div>
    )
  }
}

Members.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {

  return {
    currentUser: state.user.currentUser,
    currentGroup: state.group.currentGroup,
    memberRequests: state.group.memberRequests
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({...groupActions, ...userActions}, dispatch)
  };
}

export default connect(mapStateToProps,mapDispatchToProps)(Members);
