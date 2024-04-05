import React, { Component,PropTypes} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as userActions from '../../../../redux/actions/userActions';
import * as commentActions from '../../../../redux/actions/commentActions';
import * as groupActions from '../../../../redux/actions/groupActions';
import ApiHandler from '../../../../core/ApiHandler';
import UpdateProfilePhoto from '../../UpdateProfilePhoto';
import UserDetails from '../../../common/UserDetailsModal';
import _ from 'lodash';

class Profile extends Component {
  constructor(props){
    super(props);

    this.state={
      currentUser: props.currentUser ? props.currentUser : {},
      counter: 0,
      user:{
        name:{
          first: props.currentUser ? props.currentUser.name.first : "",
          last: props.currentUser ? props.currentUser.name.last : ""
          },
        bio:props.currentUser && props.currentUser.bio ? props.currentUser.bio : ""
      },
      member:{},
      editMode:true,
      referrals:[]
    }

    this.onClickSignOut = this.onClickSignOut.bind(this);
    this.onClickEdit = this.onClickEdit.bind(this);
    this.onChangeName = this.onChangeName.bind(this);
    this.onClickCancel = this.onClickCancel.bind(this);
    this.onClickDelete = this.onClickDelete.bind(this);
  }

  componentDidMount(){
    this.props.actions.countGroupReferrals().then(response => {
      this.setState({referrals: response});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  onClickSignOut(){
    if(window.location.href.indexOf("app") === -1){
      this.context.router.replace('/app');
    }

    this.props.actions.logout();
    window.location.reload();
  }

  onClickEdit(){

    if(this.state.editMode){
      this.updateProfile();
    } else {
      this.setState({editMode:true})
    }
  }

  onClickCancel(){
    this.props.closePeofileOption();
  }

  onClickDelete(){
    let request = ApiHandler.attachHeader({});
    this.props.actions.deleteAccount(request).then(response => {
      localStorage.clear();
      window.location.reload();
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  onChangeName(e){
    e.preventDefault();
    let user = this.state.user;

    if(e.target.name === 'bio'){
       if(e.target.value.length <= 500 ){
          user[e.target.name] = e.target.value;
       } else if( this.state.user.bio.length > e.target.value.length){
         user[e.target.name] = e.target.value;
       }
    } else {
        user.name[e.target.name] = e.target.value;
    }

    this.setState({user});
  }


    updateProfile(){
      let data = {
        name:this.state.user.name,
        bio: this.state.user.bio
      }

        let request = ApiHandler.attachHeader(data);
        this.props.actions.putUserProfile(request).then(response => {
          this.props.closePeofileOption();
        }).catch(error => {
             const errorMessage = ApiHandler.getErrorMessage(error);
             console.log(errorMessage);
          });
    }

    getRequestedMember(memberId){
      let request = ApiHandler.attachHeader({userId: memberId})
      this.props.actions.loadRequestedUser(request).then(member => {
        this.setState({showMemberModal:true,member});
      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
        console.log(errorMessage);
      });
    }

    getUserFriends(){
      let friends = <span/>;

      if(this.props.currentUser.friends && this.props.currentUser.friends.length > 0){
        friends =(
          <div className="community-container">
            {this.props.currentUser.friends.map((friend, index) => {
               return (
                 <div className="display-inline cursor-poiner" key={index} onClick={this.getRequestedMember.bind(this,friend.user.id)}>
                   <img src={friend.user.photo} alt={friend.user.name} className="community-photos" />
                   <p className="friend-name-profile">{friend.user.firstName}</p>
                  </div>)
             })
            }
          </div>
        )
      }

      return friends;

    }

  render() {
    let counter = `${this.state.user.bio.length}/500`,
        showMemberModal = <span/>;

        if(this.state.showMemberModal){
          showMemberModal = (
           <UserDetails
             currentUser={this.props.currentUser}
             user={this.state.member}
             showMemberModal={this.state.showMemberModal}
             onCloseModal={() => this.setState({showMemberModal:false})}/>
          )
        }

          return (
            <div className="profile-menu">
              {showMemberModal}
              <div className="traingular"></div>
              <p className="text-center title-profile">Edit Profile</p>
               <img
                 onClick={() => this.props.closePeofileOption()}
                 className="profile-modal-close-btn" alt="close"
                 src={require('../../../../assets/images/close-btn-black@2x.png')} />
                <UpdateProfilePhoto fromComponent={"edit-profile"}/>
                <div className="share-win-container">
                  <div>
                    <label className="tit-lable no-margin-input">First name</label>
                    <input
                      name="first"
                      value={this.state.user.name.first}
                      onChange={this.onChangeName}
                      className="edit-username"
                      type="text"/>
                  </div>
                  <div>
                    <label className="tit-lable no-margin-input">Last name</label>
                    <input
                      name="last"
                      value={this.state.user.name.last}
                      onChange={this.onChangeName}
                      className="edit-username"
                      type="text"/>
                  </div>
                </div>

                <div>
                  <label className="tit-lable no-margin-input">Tell Us About Yourself</label>
                  <textarea
                    name="bio"
                    value={this.state.user.bio}
                    onChange={this.onChangeName}
                    className="edit-bio">
                  </textarea>
                  <label className="sec-lab">{counter}</label>
                </div>

            <div className="margin-top-1">
              <div className="small-6 medium-6 large-6 columns">
                <button
                  className="edit-profile-btn"
                  onClick={this.onClickEdit}>Save</button>
              </div>
              <div className="small-6 medium-6 large-6 columns">
                <button className="profile-btn-sign">
                  <a onClick={this.onClickCancel}>Cancel</a>
                </button>
              </div>
              <div className="text-center">
                <button onClick={this.onClickDelete} className="delete-account">
                  Delete Account
                </button>
              </div>
            </div>
            </div>
          );
  }
}

Profile.contextTypes = {
 router: PropTypes.object
}


function mapStateToProps(state, ownProps) {
    return {
      currentUser: state.user.currentUser
    }
}

function mapDispatchToProps(dispatch) {
    return {
      actions: bindActionCreators({...userActions,...commentActions, ...groupActions}, dispatch)
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(Profile);
