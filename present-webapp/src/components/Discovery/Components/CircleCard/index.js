import React, {Component,PropTypes} from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as groupsActions from '../../../../redux/actions/groupsActions';
import * as groupActions from '../../../../redux/actions/groupActions';
import * as userActions from '../../../../redux/actions/userActions';
import userUtiles from '../../../../core/userUtiles';
import ApiHandler from '../../../../core/ApiHandler';
import Modal from '../../../CircleHome/components/Modal';
import groupUtiles from '../../../../core/groupUtiles';
import moment from 'moment';
import _ from 'lodash';
import './style.css';

export class CircleCard extends Component {

  constructor(props){
    super(props);

    this.state={
      currentGroup: props.currentGroup ?  props.currentGroup : {},
      savedGroups: props.savedGroups ?  props.savedGroups : [],
      circle:props.circle,
      joined: props.circle.membershipState !== 'NONE' ? true : false,
      requested: props.circle.membershipState === 'REQUESTED' || props.circle.membershipState === 'REJECTED' ? true : false,
      joinClicked:false
    }

    this.onClickJoinCircle = this.onClickJoinCircle.bind(this);
    this.onClickPermission = this.onClickPermission.bind(this);
    this.onClickConfirmLeave = this.onClickConfirmLeave.bind(this);
  }

  componentWillReceiveProps(nextProps){

    if (this.props.currentGroup !== nextProps.currentGroup) {
      this.setState({currentUser: nextProps.currentGroup});
    }

    if (this.props.savedGroups !== nextProps.savedGroups) {
      this.setState({savedGroups: nextProps.savedGroups});
    }
  }

  onClickJoinCircle(){
    let request = ApiHandler.attachHeader({groupId:this.state.circle.uuid}),
        that = this;
    if(this.state.circle.preapprove === 'ANYONE'){
      this.setState({joined:true});
    } else{
      this.setState({requested:true});
    }

    this.props.actions.saveGroupRequest(request).then(response => {

      if(response.result.result === 'ACTIVE'){
        this.setState({joined: true});
      } else if(response.result.result === 'REQUESTED' || response.result.result === 'INVITED'){
        this.setState({requested: true, showRequestedModal: true});

      } else if(response.result.result === 'REJECTED'){
        this.setState({joined: false, showRejectedModal: true});
      }
      that.props.loadRequestedGroupFromBackend(that.state.circle.uuid);
    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
          console.log(errorMessage);
    });
  }

  onClickPermission(){
    if(this.props.currentUser.id !== this.state.circle.owner.id){
      this.setState({showLeaveConfirmation:true});
      } else {
      this.setState({ownerLeave:true});
    }
  }

  goToCircle(circle){
    this.context.router.push(groupUtiles.getGroupUrl(circle));
  }

  getStaticMap(circle){
    return `https://maps.googleapis.com/maps/api/staticmap?size=1600x360&zoom=18&scale=2&maptype=roadmap&markers=color:red%7C${circle.location.latitude},${circle.location.longitude}&key=AIzaSyDAkC7ZPpRvdt2Nh1NS7fKxKJis6ZTf6N4`;
  }

  onClickConfirmLeave(){
    this.setState({showLeaveConfirmation:false, joined:false});
    let request = ApiHandler.attachHeader({groupId:this.state.circle.uuid});
    this.props.actions.leaveGroupRequest(request).then(response => {

    }).catch(error => {
      const errorMessage = ApiHandler.getErrorMessage(error);
      this.setState({joined:true});
      console.log(errorMessage);
    });
  }

  renderJoinBtn(){

    //show join button if not joined and vica versa
    if(this.state.joined){
      return(
        <div onClick={this.onClickPermission} className="opt-btn show-join-btn">
          <img alt="" src={this.state.requested ? require('../../../../assets/images/gray-join@2x.png') : require('../../../../assets/images/joined.svg')} className="general-icon"/>
        </div>
        );
    } else{
      return(
        <div className="opt-btn" onClick={this.onClickJoinCircle}>
          <img alt="" src={require('../../../../assets/images/join@2x.png')} className="general-icon"/>
        </div>
      );
    }
  }

  renderLeaveConfirmation(){
    return(
      <Modal
        show={this.state.showLeaveConfirmation}
        closeModal={() => this.setState({showLeaveConfirmation:false})}>
        <img src={require('../../../../assets/images/close-btn-white@2x.png')} alt="close" className="white-x" onClick={() => this.setState({showLeaveConfirmation:false})}/>
        <div className="confrim-container text-center ">
          <p className="confirm-text">HOLD UP</p>
          <p className="info-text">You&apos;re about to leave this circle.</p>
          <p className="info-text sec">Are you sure?</p>
          <div className="confirm-btn" onClick={this.onClickConfirmLeave}>Yes, Please</div>
          <div className="cancel-btn" onClick={() => this.setState({showLeaveConfirmation:false})}>No, I love it here!</div>
        </div>
      </Modal>
    )
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

  renderRequestedModal(){
    return(
      <Modal show={this.state.showRequestedModal} closeModal={() => this.setState({showRequestedModal:false})}>
        <div className="confrim-container text-center ">
          <p className="reques-modal-text">Success, you've requeted to join this circle.</p>
          <button className="cancel-btn got-it-btn" onClick={() => this.setState({showRequestedModal:false})}>Ok</button>
        </div>
      </Modal>
    )
  }

  renderRejectedModal(){
    return(
      <Modal show={this.state.showRejectedModal} closeModal={() => this.setState({showRejectedModal:false})}>
        <div className="confrim-container text-center ">
          <p className="reques-modal-text">Sorry, you got rejected to join this circle!</p>
          <button className="cancel-btn got-it-btn" onClick={() => this.setState({showRejectedModal:false})}>Ok</button>
        </div>
      </Modal>
    )
  }

  render() {

    let coverPhoto,
        circleTitle="",
        circleLocation="",
        {circle}=this.props;

        if(!_.isEmpty(circle)){
          // TODO: Use 50% width for non-retina displays.
          coverPhoto=circle.cover ? circle.cover.content + '=w587-h330-n-rj' : this.getStaticMap(circle);
          circleTitle=circle.title;
          circleLocation=circle.locationName;
        }

      return (
        <button className={this.props.type !== 'user-profile' ? this.props.type !== 'explore-view' ? "crad-btn add-height" : "crad-btn" : "crad-btn more-height"}>
          <div className={this.props.type === 'explore-view' ? "card-container ": "card-container for-non-explore"}>
            <div className="card-main-bg" onClick={this.goToCircle.bind(this,circle)}>
              <div className="aspect-ratio">
                <img alt="" src={coverPhoto} className={this.props.circle.cover ? "cover-photo" : "placeholder-photo"}/>
              </div>
              {!_.isEmpty(circle) && circle.schedule ?
                <div className="date-container">
                  <p className="month">{moment.unix(circle.schedule.startTime/1000).format("MMM")}</p>
                  <p className="day">{moment.unix(circle.schedule.startTime/1000).format("D")}</p>
                </div>
              : <span/>}
            </div>
            {this.state.showRequestedModal ? this.renderRequestedModal() :  <span/>}
            {this.state.showRejectedModal ? this.renderRejectedModal() :  <span/>}
            <div className="info-container cir">
              <div className="an-container" onClick={this.goToCircle.bind(this,circle)}>
                <div className={this.props.type === 'explore-view' ? "location-cont-disovery location-cont-tit": "location-cont location-cont-tit"}>
                  <span className="circle-title">
                    {circleTitle}
                  </span>
                </div>

                <div className={this.props.type === 'explore-view' ? "location-cont-disovery" : "location-cont"}>
                  <img alt="" src={require('../../../../assets/images/page-1.svg')} className={this.props.type === 'explore-view' ? "loc-icon-cir change-top" : "loc-icon-cir"}/>
                  <span className="circle-location">{circleLocation}</span>
                </div>
              </div>
              {this.state.showLeaveConfirmation ?
              this.renderLeaveConfirmation() :
              <span/>}
              {this.state.ownerLeave ?
                this.renderOwnerLeave()
              : <span/>}

              <div className="member-img-name">
              {this.renderJoinBtn()}
              {userUtiles.isUserAdmin(this.props.currentUser) ?
                <span className="memberCount">{circle.memberCount}</span>
              :
              <span/>}
              </div>
            </div>
          </div>
        </button>
      );
    }
  }

CircleCard.contextTypes = {
 router: PropTypes.object
}

function mapStateToProps(state, ownProps) {
  return {
    savedGroups: state.groups.savedGroups,
    currentUser: state.user.currentUser
  };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({...userActions,...groupsActions, ...groupActions}, dispatch)
  };
}

export default connect(mapStateToProps,mapDispatchToProps)(CircleCard);
