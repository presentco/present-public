import React, { Component} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as groupActions from '../../../../../redux/actions/groupActions';
import * as userActions from '../../../../../redux/actions/userActions';
import ApiHandler from '../../../../../core/ApiHandler';
import Modal from '../../Modal';
import _ from 'lodash';
import Spinner from 'halogen/RingLoader';

class InviteFriends extends Component {
  constructor(props){
    super(props);

    this.state={
      invitationList:[],
      invitationIds:[],
      showConfirmation:false,
      searchFriend:'',
      friends: [],
      searchFriends: [],
      presentMembers: []
    }

    this.onClickInviteFriends = this.onClickInviteFriends.bind(this);
    this.onChangeFriendName = this.onChangeFriendName.bind(this);
  }

  componentDidMount(){
    this.getMyFriends(this.props.currentUser.id);
  }

  //Called any time the Props have Changed in the Redux Store
  componentWillReceiveProps(nextProps) {
    if(this.props.memberRequests !== nextProps.memberRequests) {
        this.setState({requests: nextProps.memberRequests });
    }
  }

  getMyFriends(userId){
    let request = ApiHandler.attachHeader({userId});

    this.props.actions.getFriends(request).then(response => {
      this.setState({friends:response,allFriends:response});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  getPresentUsers(newFriends,e){
    //get the users on present
    let presentMembersNotFriends = [];
    let request = ApiHandler.attachHeader({searchText: _.startCase(_.toLower(e.target.value))})
    this.props.actions.userSearchRequest(request).then(presentMembers => {
      presentMembers.map(member=> {
        if(newFriends.length !== 0){
          for (let friend of newFriends) {
            if(friend.id !== member.id && presentMembersNotFriends.indexOf(member) === -1){
              presentMembersNotFriends.push(member);
            }
          }
        } else {
          presentMembersNotFriends = presentMembers;
        }
      });

      this.setState({presentMembers:presentMembersNotFriends, searchFriends:newFriends});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
    });
  }

  onChangeFriendName(e){
    e.preventDefault();
    let newFriends = [],
        allFriends = this.state.allFriends
    for (let friend of this.state.friends) {
      if(friend.firstName.toLowerCase().indexOf(e.target.value) > -1){
        newFriends.push(friend);
      }
    }

    if(e.target.value.length > 0){
      this.getPresentUsers(newFriends, e);
      this.setState({friends: newFriends});
    } else {

      this.setState({showFriendSearch:false, friends: allFriends})
    }

    this.setState({searchFriend:e.target.value});
  }

  onClickChooseFriend(friend){
    let invitationList = this.state.invitationList,
        invitationIds = this.state.invitationIds;
    if(invitationList.indexOf(friend) !== -1){
      let index = invitationList.indexOf(friend);
      invitationList.splice(index, 1);
      invitationIds.splice(index, 1);
    } else {
      invitationList.push(friend);
      invitationIds.push(friend.id);
    }
    this.setState({invitationList,invitationIds });
  }

  checkClass(friend){
    let customClass = "friend-name";
      if(this.state.friends.indexOf(friend) > -1){
        customClass="friend-name exists";
      } else {
        customClass="friend-name";
      }
    return customClass
  }

  checkImage(friend){
    let src = "select-radio";

    if(this.state.invitationList.indexOf(friend) > -1){
      src = "select-radio selected-radio";
    }
    return src;
  }

  onClickInviteFriends(){
    let invitationIds = this.state.invitationIds,
        request=ApiHandler.attachHeader({
          groupId:this.props.groupId,
          userIds:invitationIds
        });

    this.setState({showSpinner: true});
    this.props.actions.addMembers(request).then(response => {
      this.setState({showConfirmation:true, showSpinner:false});
      if(!this.props.fromCreateCircle){
          this.props.getCurrentGroup();
      }

      this.props.onClickClose();
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
    });
  }

  renderNonSelectedCircle(){
    let indents = [],
      lengthDifference = this.state.friends.length - this.state.invitationList.length;

    for (var i = 0; i < lengthDifference; i++) {
      indents.push(<div className='radio-select-top' key={i}></div>);
    }
    return indents;
  }

  userIsAlreadyAMember(type){
    //check if the friend, user can invite already a member or not
    let newType = type,
        id,
        that = this;

    newType.map(person => {
      id = person.user ? person.user.id : person.id;
      for (let member of this.props.members) {
        if(member.id === id){
          person.alreadyInvited = true;
        } else {
          if(!_.isEmpty(that.props.memberRequests)){
          for(let requeted of that.props.memberRequests){
            if(requeted.user.id === id){
              person.alreadyRequested = true
            }
          }
        }
      }
    }
    });

    return newType;
  }

  renderList(type){
    //render list of people either they are friends or present members
    let showFriendsList = this.userIsAlreadyAMember(type).map((friend,index) => {

      let name = friend.name,
          photo = friend.photo,
          selectedFriend = friend;

      return(
        <div className="friend-container"
             key={index}
             onClick={() => !friend.alreadyInvited && !friend.alreadyRequested ? this.onClickChooseFriend(selectedFriend) : {}}>
          <div className={friend.alreadyInvited || friend.alreadyRequested ? "non-select-friend": "select-freind"}>
            <img alt={name}
                 src={photo}
                 className={!friend.alreadyInvited && !friend.alreadyRequested? "friend-img" : "friend-img alreadry"}/>
            <div className="display-inline">
              <p className={this.checkClass(selectedFriend)}>{name}</p>
                {friend.alreadyInvited ?
                <p className="friend-name already-font">
                  Already a member
                </p> : friend.alreadyRequested ?
                <p className="friend-name already-font">
                  Requested
                </p>
                : <span style={{display:'none'}}/>}
            </div>
          </div>

          {!friend.alreadyInvited && !friend.alreadyRequested ?
            <div className={this.checkImage(friend)}></div>
            : <span/>
          }

        </div>
      )
    });
    return showFriendsList;
  }

  render(){
    let showSelectedFriends = <div/>;

    if(this.state.invitationList.length !== 0){
      showSelectedFriends = this.state.invitationList.map((selected,index) => {
        return(
          <img alt={selected.name} key={index} src={selected.photo} className="friend-img top-bar-img"/>
        )
      });
    }

    return(
      <Modal
        show={this.props.memberModalVisible}
        closeModal={() => this.props.onClickClose()}
        width={true}
        widthSize="380px">
          <div>
            <img src={require('../../../../../assets/images/close-btn-white@2x.png')}
              alt="close" className="white-x"
              onClick={() => this.props.onClickClose()}/>
            <p className="invite-title">Invite Friends To This Circle</p>
            <div className="friend-search-container">
              <p className="friends-text">Friends</p>
              <p className="friends-label">Select which friends will recieve an invitaion to join this circle on Present</p>
              <div>
                <img src={require('../../../../../assets/images/search-grey@2x.png')}
                  alt="search"
                  className="search-gray"/>
                <input
                  value={this.state.searchFriend}
                  onChange={this.onChangeFriendName}
                  className="search-friend-input"
                  placeholder="Find friends"/>
              </div>
            </div>
            {this.state.invitationList.length !== 0 ?
              <div className={this.state.invitationList.length === 0 ? "non-sel" : "selected-friend-container"}>
                {showSelectedFriends}
                {this.renderNonSelectedCircle()}
              </div>
              : <span/>
            }
            <div className="friend-confirm">
              <div className="show-friend-bar" style={{display: this.state.searchFriends.length !== 0 ? 'block' : 'none'}}>
                <p>Friends</p>
              </div>
              <div>
                {this.renderList(this.state.friends)}
              </div>

              {this.state.presentMembers.length !== 0 ?
                <div>
                  <div className="show-friend-bar">
                    <p>Present Members</p>
                  </div>
                  <div>
                  {this.renderList(this.state.presentMembers)}
                </div>
              </div> : <span/>}
            </div>

            <div className="text-center">
              {this.state.showSpinner ?
                <div className="spinner-container-custom" ref="myRef">
                    <Spinner color="#8136ec" size="70px" margin="20px"/>
                </div> :
                <button className="selected-friends"
                  onClick={this.onClickInviteFriends}
                  disabled={this.state.invitationList.length > 0 ? false : true}>
                  {this.state.invitationList.length > 0 ? "Add" : "Select"}
                </button>
              }
            </div>
          </div>
    </Modal>
    )
  }
}

function mapStateToProps(state, ownProps) {
    return {
      currentUser: state.user.currentUser,
      memberRequests: state.group.memberRequests
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({...groupActions,...userActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(InviteFriends);
