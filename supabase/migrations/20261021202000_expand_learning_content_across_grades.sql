-- Expand published starter learning packs beyond Grade 4.
-- These are paraphrased practice outcomes, not a replacement for full KICD designs.
-- Content is starter material and should be reviewed by a curriculum editor.
do $$
declare
 g record; a record; v_source uuid; v_strand uuid; v_sub uuid;
 v_title text; v_desc text; v_sub1 text; v_sub2 text;
 v_o1 text; v_o2 text; v_o3 text; v_o4 text; v_act1 text; v_act2 text; n int;
begin
 select id into v_source from public.curriculum_sources
 where source_url='https://kicd.ac.ke/cbc-materials/curriculum-designs/regular-curriculum-designs/' limit 1;
 for g in select id,name from public.curriculum_grades where id in ('g1','g2','g3','g5','g6','g7','g8','g9') order by sort_order loop
  n:=substring(g.id from 2)::int;
  for a in select la.id from public.learning_areas la join public.grade_learning_areas gla on gla.learning_area_id=la.id
    where gla.grade_id=g.id and la.id in ('mathematics','english','english_activities','kiswahili','science_technology','integrated_science','social_studies','agriculture_nutrition','creative_activities')
    group by la.id loop
   if a.id='mathematics' then
    v_title:=case when n<=3 then 'Numbers, patterns and everyday problem solving' when n<=6 then 'Number operations and measurement' else 'Mathematical reasoning and real-life applications' end;
    v_desc:='Grade-specific practice using mathematical ideas to solve familiar everyday problems.';
    v_sub1:=case when n<=3 then 'Counting, comparing and number patterns' when n<=6 then 'Place value and the four operations' else 'Multi-step problems, ratios and number relationships' end;
    v_sub2:=case when n<=3 then 'Shapes, position and simple measurement' when n<=6 then 'Fractions, measures and geometric reasoning' else 'Measurement, geometry and interpreting data' end;
    v_o1:='Use age-appropriate numbers and representations to explain a mathematical idea and solve a familiar problem.';
    v_o2:='Describe a number pattern and check a solution using a second method or concrete materials.';
    v_o3:='Measure or compare objects using suitable units and record the results clearly.';
    v_o4:='Explain the steps used to solve a practical problem and check whether the answer is reasonable.';
    v_act1:='Use counters, locally available objects or drawings to model a number problem, then explain the method.';
    v_act2:='Measure objects around the classroom or home, record the results, and compare findings with a partner.';
   elsif a.id in ('english','english_activities') then
    v_title:=case when n<=3 then 'Listening, speaking, reading and writing' when n<=6 then 'Reading comprehension and written communication' else 'Comprehension, language use and purposeful communication' end;
    v_desc:='Age-appropriate English language practice for communicating ideas clearly in familiar contexts.';
    v_sub1:=case when n<=3 then 'Sounds, words and simple sentences' when n<=6 then 'Reading for meaning and vocabulary' else 'Inference, vocabulary and critical reading' end;
    v_sub2:=case when n<=3 then 'Building sentences and short messages' when n<=6 then 'Paragraphs, descriptions and short compositions' else 'Organising ideas and writing for different purposes' end;
    v_o1:='Listen to or read a level-appropriate text and identify its main idea and relevant details.';
    v_o2:='Use familiar vocabulary and correct sentence structures to communicate an idea orally or in writing.';
    v_o3:='Organise ideas in a short piece of writing appropriate to the learner’s grade and purpose.';
    v_o4:='Review a spoken or written response and make a useful improvement for clarity.';
    v_act1:='Read a short age-appropriate passage, retell its main idea and discuss two details with a partner.';
    v_act2:='Write a short message, description or paragraph, then revise it using a simple checklist.';
   elsif a.id='kiswahili' then
    v_title:=case when n<=3 then 'Kusikiliza, kuzungumza, kusoma na kuandika' when n<=6 then 'Ufahamu na matumizi ya lugha' else 'Ufahamu, sarufi na mawasiliano' end;
    v_desc:='Mazoezi ya Kiswahili yanayolingana na kiwango cha darasa na mawasiliano ya kila siku.';
    v_sub1:=case when n<=3 then 'Msamiati na sentensi rahisi' when n<=6 then 'Kusoma na kuelewa matini' else 'Kufasiri matini na kupanua msamiati' end;
    v_sub2:=case when n<=3 then 'Kuandika maneno na ujumbe mfupi' when n<=6 then 'Kuandika aya na maelezo' else 'Kuandika kwa madhumuni na hadhira tofauti' end;
    v_o1:='Kusikiliza au kusoma matini inayolingana na darasa na kueleza wazo kuu pamoja na maelezo muhimu.';
    v_o2:='Kutumia msamiati na miundo ya sentensi inayofaa katika mawasiliano ya mdomo au maandishi.';
    v_o3:='Kuandika ujumbe, maelezo au aya fupi yenye mpangilio unaoeleweka.';
    v_o4:='Kusoma upya kazi na kurekebisha makosa ili kuboresha uwazi wa ujumbe.';
    v_act1:='Soma matini fupi, eleza wazo kuu na jadili maana ya maneno mapya.';
    v_act2:='Andika ujumbe au aya fupi kisha uiboreshe kwa kutumia orodha ya ukaguzi.';
   elsif a.id in ('science_technology','integrated_science') then
    v_title:=case when n<=3 then 'Observation, materials and the living world' when n<=6 then 'Scientific investigation and the environment' else 'Scientific inquiry, systems and responsible technology use' end;
    v_desc:='Practical science activities that build observation, questioning, safe investigation and evidence-based explanation.';
    v_sub1:=case when n<=3 then 'Living things and their needs' when n<=6 then 'Living systems and environmental care' else 'Systems, interactions and environmental sustainability' end;
    v_sub2:=case when n<=3 then 'Everyday materials and simple observations' when n<=6 then 'Materials, changes and simple investigations' else 'Planning investigations and interpreting evidence' end;
    v_o1:='Observe a familiar living thing or natural process and describe evidence using suitable scientific words.';
    v_o2:='Identify a question that can be explored safely through observation or a simple investigation.';
    v_o3:='Record observations in words, drawings, a table or another grade-appropriate format.';
    v_o4:='Use recorded evidence to explain a finding and suggest one way to care for the environment.';
    v_act1:='Observe a plant, animal or local habitat and record visible features without disturbing living things.';
    v_act2:='Compare safe everyday materials, record observable properties and share a conclusion based on evidence.';
   elsif a.id='social_studies' then
    v_title:=case when n<=3 then 'Self, family, community and place' when n<=6 then 'People, resources and responsible citizenship' else 'Communities, environments and civic responsibility' end;
    v_desc:='Grade-appropriate exploration of community life, place, resources and responsible participation.';
    v_sub1:=case when n<=3 then 'Family, school and community roles' when n<=6 then 'Local environments and shared resources' else 'Community change, resources and citizenship' end;
    v_sub2:=case when n<=3 then 'Places, directions and simple maps' when n<=6 then 'Maps, livelihoods and cultural diversity' else 'Interpreting information about places and communities' end;
    v_o1:='Describe roles people play in a community and explain one way to contribute responsibly.';
    v_o2:='Identify features of a familiar place and represent or describe their locations clearly.';
    v_o3:='Explain how people use shared resources and why they should be cared for.';
    v_o4:='Compare two community perspectives respectfully and support an idea with an example.';
    v_act1:='Draw a simple map of a familiar place and label important features using a key or symbols.';
    v_act2:='Discuss a shared community resource and propose practical steps for using it responsibly.';
   elsif a.id='agriculture_nutrition' then
    v_title:=case when n<=3 then 'Plants, food and caring for our environment' when n<=6 then 'Food production and sustainable resource use' else 'Sustainable agriculture and food systems' end;
    v_desc:='Practical learning about food, plants, nutrition and responsible use of local resources.';
    v_sub1:=case when n<=3 then 'Growing and caring for plants' when n<=6 then 'Crop care and soil protection' else 'Sustainable production and resource management' end;
    v_sub2:=case when n<=3 then 'Healthy foods and cleanliness' when n<=6 then 'Food choices, hygiene and nutrition' else 'Food safety, nutrition and sustainable practices' end;
    v_o1:='Describe the basic needs of a plant and demonstrate a safe, suitable way to care for it.';
    v_o2:='Identify practices that help protect soil, water or other resources in the local environment.';
    v_o3:='Identify balanced food choices and explain a relevant food hygiene practice.';
    v_o4:='Plan a simple practical activity using locally available materials while observing safety.';
    v_act1:='Grow or observe a plant and keep a short record of its changes and care needs.';
    v_act2:='Sort pictures or examples of foods into useful groups and discuss safe food handling.';
   else
    v_title:=case when n<=3 then 'Creating, movement and expression' when n<=6 then 'Creative expression and physical wellbeing' else 'Creative practice, movement and wellbeing' end;
    v_desc:='Practical activities that develop expression, coordination, collaboration and safe participation.';
    v_sub1:='Creative skills and communicating ideas'; v_sub2:='Movement, coordination and personal wellbeing';
    v_o1:='Use available materials or movement to express an idea safely and creatively.';
    v_o2:='Describe choices made during a creative task and respond respectfully to others’ work.';
    v_o3:='Demonstrate a movement skill appropriate to the learner’s ability and available space.';
    v_o4:='Identify simple practices that support safety, cooperation and personal wellbeing.';
    v_act1:='Create a picture, rhythm or simple performance from locally available materials.';
    v_act2:='Practise a safe movement sequence with a partner and reflect on cooperation.';
   end if;
   select id into v_strand from public.curriculum_strands where grade_id=g.id and learning_area_id=a.id and title=v_title limit 1;
   if v_strand is null then
    insert into public.curriculum_strands(grade_id,learning_area_id,title,sequence,description,source_id,content_status,is_published)
    values(g.id,a.id,v_title,1,v_desc,v_source,'published',true) returning id into v_strand;
   else update public.curriculum_strands set is_published=true,content_status='published' where id=v_strand; end if;
   select id into v_sub from public.curriculum_sub_strands where strand_id=v_strand and title=v_sub1 limit 1;
   if v_sub is null then insert into public.curriculum_sub_strands(strand_id,title,sequence,description,source_id,is_published)
    values(v_strand,v_sub1,1,'Starter topic for '||g.name||'; verify against the current KICD design.',v_source,true) returning id into v_sub; end if;
   insert into public.learning_outcomes(sub_strand_id,outcome_text,outcome_type,sequence,source_id,is_published)
    select v_sub,x.t,'specific',x.n,v_source,true from (values(v_o1,1),(v_o2,2)) x(t,n)
    where not exists(select 1 from public.learning_outcomes lo where lo.sub_strand_id=v_sub and lo.outcome_text=x.t);
   insert into public.inquiry_questions(sub_strand_id,question_text,sequence,source_id,is_published)
    select v_sub,v_act1,1,v_source,true where not exists(select 1 from public.inquiry_questions iq where iq.sub_strand_id=v_sub and iq.question_text=v_act1);
   insert into public.learning_activities(sub_strand_id,activity_text,resources,sequence,source_id,is_published)
    select v_sub,v_act1,'["exercise book","pencil","locally available materials"]'::jsonb,1,v_source,true
    where not exists(select 1 from public.learning_activities x where x.sub_strand_id=v_sub and x.activity_text=v_act1);
   select id into v_sub from public.curriculum_sub_strands where strand_id=v_strand and title=v_sub2 limit 1;
   if v_sub is null then insert into public.curriculum_sub_strands(strand_id,title,sequence,description,source_id,is_published)
    values(v_strand,v_sub2,2,'Starter topic for '||g.name||'; verify against the current KICD design.',v_source,true) returning id into v_sub; end if;
   insert into public.learning_outcomes(sub_strand_id,outcome_text,outcome_type,sequence,source_id,is_published)
    select v_sub,x.t,'specific',x.n,v_source,true from (values(v_o3,1),(v_o4,2)) x(t,n)
    where not exists(select 1 from public.learning_outcomes lo where lo.sub_strand_id=v_sub and lo.outcome_text=x.t);
   insert into public.inquiry_questions(sub_strand_id,question_text,sequence,source_id,is_published)
    select v_sub,v_act2,1,v_source,true where not exists(select 1 from public.inquiry_questions iq where iq.sub_strand_id=v_sub and iq.question_text=v_act2);
   insert into public.learning_activities(sub_strand_id,activity_text,resources,sequence,source_id,is_published)
    select v_sub,v_act2,'["exercise book","pencil","locally available materials"]'::jsonb,1,v_source,true
    where not exists(select 1 from public.learning_activities x where x.sub_strand_id=v_sub and x.activity_text=v_act2);
  end loop;
 end loop;
end $$;