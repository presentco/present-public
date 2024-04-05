import React, { Component} from 'react';
import {connect} from 'react-redux';
import {bindActionCreators} from 'redux';
import * as groupActions from '../../../../../redux/actions/groupActions';
import * as groupsActions from '../../../../../redux/actions/groupsActions';
import InviteFriends from './inviteFriends';
import CopyToClipboard from 'react-copy-to-clipboard';
import ApiHandler from '../../../../../core/ApiHandler';
import Modal from '../../Modal';
import _ from 'lodash';
import {
  ShareButtons
} from 'react-share';

const {
  FacebookShareButton,
  TwitterShareButton
} = ShareButtons;

class CircleOptions extends Component {

  constructor(props){
    super(props);
    this.state={
      showLeaveConfirmation:false,
      inLeaveMsg:false,
      copied:false,
      memberModalVisible:false,
      ownerLeave:false,
      joined: props.currentGroup.membershipState !== 'NONE' ?  true : false,
      requested: props.currentGroup.membershipState === 'REQUESTED' || props.currentGroup.membershipState === 'REJECTED' ? true : false,
      requests:props.memberRequests ? props.memberRequests : []
    }

    this.onClickLeaveCirlce = this.onClickLeaveCirlce.bind(this);
    this.onClickPermission = this.onClickPermission.bind(this);
    this.onClickConfirmLeave = this.onClickConfirmLeave.bind(this);
    this.onClickJoinCircle = this.onClickJoinCircle.bind(this);

  }



  componentWillReceiveProps(nextProps){
    if (this.props.currentGroup !== nextProps.currentGroup) {
      this.setState({currentGroup: nextProps.currentGroup});
        this.getJoinTitle(nextProps.currentGroup);
    }

    if (this.props.memberRequests !== nextProps.memberRequests) {
      this.setState({requests: nextProps.memberRequests});
      this.getJoinedTitle(nextProps.memberRequests);
    }

    if (this.props.savedGroups !== nextProps.savedGroups) {
        this.setState({savedGroups: nextProps.savedGroups });
    }
  }

  getJoinedTitle(requests){
    let that = this;

    if(requests.length > 0){
      for(let request of requests){
        if(!_.isEmpty(that.props.currentUser) && that.props.currentUser.id === request.user.id){
          that.setState({title: 'Requested', joined:true});
          break;
        }
      }
    }
  }

  loadUserSavedGroups() {
    let requestTest = ApiHandler.attachHeader({
        location: this.props.currentUser.home.location
    });

    this.props.actions.getSavedGroups(requestTest, this.props.currentUser.home).then(response => {
      this.setState({savedGroups: response});
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      console.log(errorMessage);
    });
  }

  onClickConfirmLeave(){

    this.setState({showLeaveConfirmation:false, joined:false});
    let request = ApiHandler.attachHeader({groupId:this.props.currentGroup.uuid});
    this.props.actions.leaveGroupRequest(request).then(response => {
      this.setState({joined:false, showSetting: false, showFeedback:true, title: "Join"});
      this.loadUserSavedGroups();
      this.props.getCurrentGroup();
      this.props.getMembershipRequests(this.props.currentGroup.uuid);
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
          console.log(errorMessage);
    });
  }

  onClickJoinCircle(){

      let request = ApiHandler.attachHeader({groupId:this.props.currentGroup.uuid}),
          title = "",
          that = this;

          if(this.props.currentGroup.preapprove === 'ANYONE'){
            this.setState({joined:true});
          } else{
            this.setState({requested:true});
          }

      this.props.actions.saveGroupRequest(request).then(response => {

        if(response.result.result === 'ACTIVE'){
          title = 'Joined';
          this.setState({joined: true});
        } else if(response.result.result === 'REQUESTED' || response.result.result === 'REJECTED'){
          title = "Requested";
        }

        if(title === "Rejected"){
          this.setState({requested: true});
        }
        this.setState({joined: true,title});

        if(that.props.isUserAuthorized){
          this.props.getMembershipRequests(this.props.currentGroup.uuid);
          this.getJoinedTitle(this.state.requests)
        }

        this.loadUserSavedGroups();
        this.props.getCurrentGroup();
      }).catch(error => {
        const errorMessage = ApiHandler.getErrorMessage(error);
            console.log(errorMessage);
      });


  }


  onClickPermission(){

      if(this.props.currentUser.id !== this.props.currentGroup.owner.id){
      this.setState({inLeaveMsg:!this.state.inLeaveMsg});
      } else{
        this.setState({ownerLeave:true});
      }

  }

  onClickLeaveCirlce(){
    //but first show the confirmation msg
    this.setState({showLeaveConfirmation:true, inLeaveMsg:false});
  }

  onClickSelectReason(reason){
    let reasons = this.state.reasons;
    for (let prop in reasons) {
        if(prop === reason){
          reasons[prop] = !reasons[prop];
        } else {
          reasons[prop] = false;
        }
    }
    this.setState({reasons});
  }


  renderOwnerLeave(){
    return(
      <Modal show={this.state.ownerLeave} closeModal={() => this.setState({ownerLeave:false})}>
        <div className="confrim-container text-center ">
          <p className="confirm-text">You can&apos;t leave your own circle!</p>
          <button className="cancel-btn" onClick={() => this.setState({ownerLeave:false})}>Close</button>
        </div>
      </Modal>
    )
  }

  getJoinTitle(group){

    let newTitle = '';
    if(group.membershipState === 'ACTIVE'){
      newTitle = "Joined";
    } else {
      newTitle = _.startCase(_.toLower(group.membershipState));
    }

    this.setState({joined:group.joined,
    title: newTitle});

  }

  renderJoinBtn(){

    //show join button if not joined and vica versa
    if(this.state.joined){
      return(
        <button onClick={this.onClickPermission} className="opt-btn jon-btn show-join-btn">
          <img alt="" src={this.state.requested ? require('../../../../../assets/images/gray-join@2x.png') : require('../../../../../assets/images/joined.svg')} className="general-icon"/>
          <span className="option-title">{this.state.title}</span>
        </button>
        );
    } else {
      return(
        <button className="opt-btn jon-btn" onClick={this.onClickJoinCircle}>
          <img alt="" src={require('../../../../../assets/images/join@2x.png')} className="general-icon"/>
          <span className="option-title">{this.props.currentGroup.preapprove === 'INVITE_ONLY' ? "Request" : "Join"}</span>
        </button>
      );
    }
  }

  renderLeaveMsg(){
    return(
      <img alt=""
        className="leave-cir-img"
        src={require('../../../../../assets/images/leave-circle@2x.png')} onClick={this.onClickLeaveCirlce}/>
    )
  }

  renderLeaveConfirmation(){
    return(
      <Modal
        show={this.state.showLeaveConfirmation}
        closeModal={() => this.setState({showLeaveConfirmation:false})}>
        <img src={require('../../../../../assets/images/close-btn-white@2x.png')} alt="close" className="white-x" onClick={() => this.setState({showLeaveConfirmation:false})}/>
        <div className="confrim-container text-center ">
          <p className="confirm-text">HOLD UP</p>
          <p className="info-text">You&apos;re about to leave this circle.</p>
          <p className="info-text sec">Are you sure?</p>
          <button className="confirm-btn" onClick={this.onClickConfirmLeave}>Yes, Please</button>
          <button className="cancel-btn" onClick={() => this.setState({showLeaveConfirmation:false})}>No, I love it here!</button>
        </div>
      </Modal>
    )
  }

  renderShowCopied(){

    let link = `mailto:yourfriend@example.com?Subject=Join%20me%20on%20Present!%20&body=Hi%20friends!%20Join%20me%20in%20the%20"${this.props.currentGroup.title}"%20circle%20on%20Present!%20%0D%0A%0D%0A${this.props.currentGroup.url}`,
        currentGroup = this.props.currentGroup;
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
                        <img alt="link" className="icon-link" src={require('../../../../../assets/images/link@2x.png')}/>
                        <p className="icon-title">Link</p>
                      </div>
                    </CopyToClipboard>
                    <div className="text-center cursor-poiner" onClick={this.shareToFb}>
                      <FacebookShareButton
                       url={currentGroup.url}
                       quote={`Hi friends! Join me in the "${currentGroup.title}" circle on Present! ${currentGroup.url}`}>
                       <img alt="facebook" className="icon-link" src={require('../../../../../assets/images/facebook-link@2x.png')}/>
                       <p className="icon-title">Facebook</p>
                     </FacebookShareButton>
                    </div>
                    <div className="twitter-email-div">
                      <a href={link} className="text-center cursor-poiner">
                        <img alt="email" className="icon-link" src={require('../../../../../assets/images/email-link@2x.png')}/>
                        <p className="icon-title">Email</p>
                      </a>
                    </div>
                    <div className="text-center cursor-poiner">
                      <TwitterShareButton
                       url={currentGroup.url}
                       title={`Hi friends! Join me in the "${currentGroup.title}" circle on Present! ${currentGroup.url}`}>
                       <img alt="twitter" className="icon-link" src={require('../../../../../assets/images/twitter-link@2x.png')}/>
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
             <img src={require('../../../../../assets/images/close-btn-white@2x.png')}
            className="white-x"
            alt="close"
            onClick={() => this.setState({linkCopied:false})}/>
           <div className="confrim-container text-center ">
             <p className="confirm-text is-success">Copied link to clipboard</p>
             <p className="info-text">Share this circle with friends!</p>
             <p>{this.state.linkText}</p>
           </div>
      </Modal>
    )
  }

  renderMemberInviteModal(){
    return(
      <InviteFriends
        onClickClose={()=> this.setState({memberModalVisible:false})}
        currentUser={this.props.currentUser}
        memberModalVisible={this.state.memberModalVisible}
        groupId={this.props.currentGroup.uuid}
        members={this.props.members}
        getCurrentGroup={() => this.props.getCurrentGroup()}/>
    )
  }

  render(){
    return(
      <div className="group-option-container">
        <button className="opt-btn" onClick={() => this.setState({shareClicked: true})}>
          <img alt="" src={require('../../../../../assets/images/share-icon@2x.png')} className="general-icon"/>
          <span className="option-title">Share</span>
        </button>

          <button className={!_.isEmpty(this.state.currentGroup) && this.state.currentGroup.joined ? "opt-btn" : "opt-btn disabled-btn-cursor"} onClick={()=>this.setState({memberModalVisible:this.state.currentGroup.joined})}>
            <img alt="invite" src={require('../../../../../assets/images/invite-icon@2x.png')} className="general-icon"/>
            <span className={!_.isEmpty(this.state.currentGroup) && this.state.currentGroup.joined ? "option-title" : "option-title disabled-text"}>Add</span>
          </button>

            {this.state.ownerLeave ? this.renderOwnerLeave() : <span/>}
            {this.renderJoinBtn()}
            {this.state.inLeaveMsg ? this.renderLeaveMsg() : <span/>}
            {this.state.showLeaveConfirmation ? this.renderLeaveConfirmation() : <span/>}
            {this.state.shareClicked ? this.renderShowCopied() : <span/>}
            {this.state.linkCopied ? this.renderLinkshared() : <span/>}
            {this.state.memberModalVisible ?  this.renderMemberInviteModal() : <span/>}

      </div>
    )
  }
}

function mapStateToProps(state, ownProps) {
    return {
      currentGroup: state.group.currentGroup,
      savedGroups: state.groups.savedGroups,
      memberRequests: state.group.memberRequests
    }
}

function mapDispatchToProps(dispatch) {
    return {
        actions: bindActionCreators({...groupActions,...groupsActions}, dispatch)
    }
}


export default connect(mapStateToProps, mapDispatchToProps)(CircleOptions);
