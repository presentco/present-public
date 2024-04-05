import React, {Component} from 'react';
import _ from 'lodash';
import groupUtiles from '../../../core/groupUtiles';
import Cards from '../Components/Cards';
import CreateCircleCard from '../Components/CreateCircleCard.js';


export class ExploreView extends Component {

  constructor(props){
    super(props);

    this.state={

    }
  }

  renderCreateCircleCard(title,addPhoto){
    return (
      <div className="small-12 medium-6 large-3 no-padding-left end columns">
        <div className="card-view" onClick={() => this.props.onClickCreateCircle(title)}>
          <button className="crad-btn">
            <div className="card-container">
              <div className="card-main-bg">
                <div className="aspect-ratio">
                  <img alt={title} src={addPhoto} className="cover-photo"/>
                </div>
              </div>
              <div className="info-container cir">
                <div className="an-container">
                  <div className="location-cont location-cont-tit">
                    <span className="circle-title">
                      Create A Circle
                    </span>
                  </div>
                  <span className="here-text">Here</span>
                </div>
              </div>
            </div>
          </button>
        </div>
      </div>
    )
  }

  renderExploreView(preface, title){
    let that =  this,
        newTitle = title === 'Nearby' ?  'Trending' : title;

    let renderExploreView = <span/>,
        addPhoto = title !== 'Nearby' ? title === 'Live' ? `https://present.co/images/Grow-exno@2x.png` : `https://present.co/images/${title}-exno@2x.png` : `https://present.co/images/All-exno@2x.png`;

      let filteredGroups = title === 'Nearby' ? !_.isEmpty(this.props.nearByGroups) ? this.props.nearByGroups : [] : groupUtiles.filterGroups(title, that.props.nearByGroups);

      if(title === 'New' && _.isEmpty(filteredGroups)){
        renderExploreView = <span/>;
      } else {
        if(groupUtiles.isWomenOwnedCircle(filteredGroups,title) || filteredGroups.length === 0){
          renderExploreView = (
            <div className="category-container">
              <div>
                <div className="small-10 medium-10 large-10 no-padding columns">
                  <p className="let-text">
                    {preface}
                  </p>
                  <p className="vibe-text">
                    {newTitle}
                  </p>
                </div>
                {filteredGroups.length > 0 ?
                  <div className="small-2 medium-2 large-2 title-category no-padding columns">
                    <p className="view-all-btn pointer" onClick={() => this.props.onClickViewAll(title)}>VIEW ALL</p>
                  </div>
                  : <span/>
                }
              </div>

              <div className="small-12 medium-12 large-12 no-padding columns">
               <div className="category-wrappers">
                 {!_.isEmpty(filteredGroups) ?
                   <Cards
                    loadRequestedGroupFromBackend={(uuid) => this.props.loadRequestedGroupFromBackend(uuid)}
                     type="explore-view"
                     savedGroups={this.props.savedGroups}
                     currentGroup={this.props.currentGroup}
                     filter={title}
                     nearByGroups={filteredGroups}
                     searchResult={this.props.searchResult}
                     addPhoto={addPhoto}
                     onClickCreateCircle={() => this.props.onClickCreateCircle(title)}/> :
                     <CreateCircleCard
                       type="explore-view"
                       title={title}
                       addPhoto={addPhoto}
                       onClickCreateCircle={() => this.props.onClickCreateCircle(title)}/>
                 }
              </div>
            </div>
          </div>)
        }
      }

    return renderExploreView;
  }

  getSelectedSpace(){
    let newSelectedSpace = "";

    try {
        const jsonCurrentSpace = localStorage.getItem('selectedSpace');

        if (jsonCurrentSpace !== undefined) {
            const selectedSpace = JSON.parse(jsonCurrentSpace)
            if (selectedSpace) {
                newSelectedSpace = selectedSpace.name
            }
        }
    } catch (e) {
        // localStorage.removeItem('currentUser');
    }

    return newSelectedSpace;
  }


    render() {

      return (
        <div className="explore-container">
          {this.renderExploreView('circles', 'Nearby')}
          {this.renderExploreView('See what’s', 'New')}
          {this.renderExploreView('Discuss in', 'Work')}
          {this.renderExploreView('Find nearby', 'Communities')}
          {this.renderExploreView('Attend', 'Events')}
          {this.renderExploreView('Share your love for', 'Friends')}
          {this.renderExploreView('Get motivated with', 'Exercise')}
          {this.renderExploreView('Discover', 'Eat & Drink')}
          {this.renderExploreView('The latest in', 'Live')}
          {this.renderExploreView('Give back', 'Volunteer')}
          {this.renderExploreView('Rep your', 'Style')}
          {this.getSelectedSpace() === "Women Only" ? this.renderExploreView('Find & Support', 'Woman Owned') : <span/>}
        </div>
      );
      }
}

export default ExploreView;
